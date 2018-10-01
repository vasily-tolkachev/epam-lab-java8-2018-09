package lambda.part3.exercise;

import lambda.data.Employee;
import lambda.data.JobHistoryEntry;
import lambda.data.Person;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"unused", "ConstantConditions"})
class Exercise4 {

    private static class LazyCollectionHelper<T, R> {
        private final List<T> source;
        private final Function<T, List<R>> function;

        private LazyCollectionHelper(List<T> source, Function<T, List<R>> function) {
            this.source = source;
            this.function = function;
        }

        public static <T> LazyCollectionHelper<T, T> from(List<T> list) {
            return new LazyCollectionHelper<>(list, Collections::singletonList);
        }

        public <U> LazyCollectionHelper<T, U> flatMap(Function<R, List<U>> flatMapping) {
            return getCollectionHelperWithNewFunction((list, r) -> list.addAll(flatMapping.apply(r)));
        }

        public <U> LazyCollectionHelper<T, U> map(Function<R, U> mapping) {
            return getCollectionHelperWithNewFunction((list, r) -> list.add(mapping.apply(r)));
        }

        public List<R> force() {
            List<R> result = new ArrayList<>();
            source.forEach(t -> result.addAll(function.apply(t)));
            return result;
        }

        private <U> LazyCollectionHelper<T, U> getCollectionHelperWithNewFunction(BiConsumer<List<U>, R> adder) {
            Function<T, List<U>> newFunction = this.function.andThen(list -> {
                List<U> listU = new ArrayList<>();
                list.forEach(r -> adder.accept(listU, r));
                return listU;
            });
            return new LazyCollectionHelper<>(source, newFunction);
        }
    }

    @Test
    void mapEmployeesToCodesOfLetterTheirPositionsUsingLazyFlatMapHelper() {
        List<Employee> employees = getEmployees();

        List<Integer> codes = LazyCollectionHelper
                .from(employees)
                .flatMap(Employee::getJobHistory)
                .map(JobHistoryEntry::getPosition)
                .flatMap(Exercise4::calcCodes)
                .force();

        assertThat(codes, Matchers.contains(calcCodes("dev", "dev", "tester", "dev", "dev", "QA", "QA", "dev", "tester", "tester", "QA", "QA", "QA", "dev").toArray()));
    }

    private static List<Integer> calcCodes(String...strings) {
        List<Integer> codes = new ArrayList<>();
        for (String string : strings) {
            for (char letter : string.toCharArray()) {
                codes.add((int) letter);
            }
        }
        return codes;
    }

    private static List<Employee> getEmployees() {
        return Arrays.asList(
                new Employee(
                        new Person("Иван", "Мельников", 30),
                        Arrays.asList(
                                new JobHistoryEntry(2, "dev", "EPAM"),
                                new JobHistoryEntry(1, "dev", "google")
                        )),
                new Employee(
                        new Person("Александр", "Дементьев", 28),
                        Arrays.asList(
                                new JobHistoryEntry(1, "tester", "EPAM"),
                                new JobHistoryEntry(1, "dev", "EPAM"),
                                new JobHistoryEntry(1, "dev", "google")
                        )),
                new Employee(
                        new Person("Дмитрий", "Осинов", 40),
                        Arrays.asList(
                                new JobHistoryEntry(3, "QA", "yandex"),
                                new JobHistoryEntry(1, "QA", "mail.ru"),
                                new JobHistoryEntry(1, "dev", "mail.ru")
                        )),
                new Employee(
                        new Person("Анна", "Светличная", 21),
                        Collections.singletonList(
                                new JobHistoryEntry(1, "tester", "T-Systems")
                        )),
                new Employee(
                        new Person("Игорь", "Толмачёв", 50),
                        Arrays.asList(
                                new JobHistoryEntry(5, "tester", "EPAM"),
                                new JobHistoryEntry(6, "QA", "EPAM")
                        )),
                new Employee(
                        new Person("Иван", "Александров", 33),
                        Arrays.asList(
                                new JobHistoryEntry(2, "QA", "T-Systems"),
                                new JobHistoryEntry(3, "QA", "EPAM"),
                                new JobHistoryEntry(1, "dev", "EPAM")
                        ))
        );
    }

}
