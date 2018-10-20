package streams.part2.exercise;

import lambda.data.Employee;
import lambda.data.JobHistoryEntry;
import lambda.data.Person;
import lombok.Value;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("ConstantConditions")
class Exercise2 {

    /**
     * Преобразовать список сотрудников в отображение [компания -> множество людей, когда-либо работавших в этой компании].
     * <p>
     * Входные данные:
     * [
     * {
     * {Иван Мельников 30},
     * [
     * {2, dev, "EPAM"},
     * {1, dev, "google"}
     * ]
     * },
     * {
     * {Александр Дементьев 28},
     * [
     * {2, tester, "EPAM"},
     * {1, dev, "EPAM"},
     * {1, dev, "google"}
     * ]
     * },
     * {
     * {Дмитрий Осинов 40},
     * [
     * {3, QA, "yandex"},
     * {1, QA, "EPAM"},
     * {1, dev, "mail.ru"}
     * ]
     * },
     * {
     * {Анна Светличная 21},
     * [
     * {1, tester, "T-Systems"}
     * ]
     * }
     * ]
     * <p>
     * Выходные данные:
     * [
     * "EPAM" -> [
     * {Иван Мельников 30},
     * {Александр Дементьев 28},
     * {Дмитрий Осинов 40}
     * ],
     * "google" -> [
     * {Иван Мельников 30},
     * {Александр Дементьев 28}
     * ],
     * "yandex" -> [ {Дмитрий Осинов 40} ]
     * "mail.ru" -> [ {Дмитрий Осинов 40} ]
     * "T-Systems" -> [ {Анна Светличная 21} ]
     * ]
     */
    @Test
    void employersStuffList() {
        List<Employee> employees = getEmployees();

        Map<String, Set<Person>> result = employees.stream()
                .flatMap(employee -> employee.getJobHistory()
                        .stream()
                        .map(entry -> new Pair(entry.getEmployer(), employee.getPerson()))
                )
                .collect(groupingBy(Pair::getEmployer, mapping(Pair::getPerson, toSet())));

        assertThat(result, hasEntry((is("yandex")), contains(employees.get(2).getPerson())));
        assertThat(result, hasEntry((is("mail.ru")), contains(employees.get(2).getPerson())));
        assertThat(result, hasEntry((is("google")), containsInAnyOrder(employees.get(0).getPerson(), employees.get(1).getPerson())));
        assertThat(result, hasEntry((is("T-Systems")), containsInAnyOrder(employees.get(3).getPerson(), employees.get(5).getPerson())));
        assertThat(result, hasEntry((is("EPAM")), containsInAnyOrder(
                employees.get(0).getPerson(),
                employees.get(1).getPerson(),
                employees.get(4).getPerson(),
                employees.get(5).getPerson()))
        );
    }

    @Value
    class Pair {
        String employer;
        Person person;
    }


    /**
     * Преобразовать список сотрудников в отображение [компания -> множество людей, начавших свою карьеру в этой компании].
     * <p>
     * Пример.
     * <p>
     * Входные данные:
     * [
     * {
     * {Иван Мельников 30},
     * [
     * {2, dev, "EPAM"},
     * {1, dev, "google"}
     * ]
     * },
     * {
     * {Александр Дементьев 28},
     * [
     * {2, tester, "EPAM"},
     * {1, dev, "EPAM"},
     * {1, dev, "google"}
     * ]
     * },
     * {
     * {Дмитрий Осинов 40},
     * [
     * {3, QA, "yandex"},
     * {1, QA, "EPAM"},
     * {1, dev, "mail.ru"}
     * ]
     * },
     * {
     * {Анна Светличная 21},
     * [
     * {1, tester, "T-Systems"}
     * ]
     * }
     * ]
     * <p>
     * Выходные данные:
     * [
     * "EPAM" -> [
     * {Иван Мельников 30},
     * {Александр Дементьев 28}
     * ],
     * "yandex" -> [ {Дмитрий Осинов 40} ]
     * "T-Systems" -> [ {Анна Светличная 21} ]
     * ]
     */
    @Test
    void indexByFirstEmployer() {
        List<Employee> employees = getEmployees();

        Map<String, Set<Person>> result = employees.stream()
                .map(employee -> new Pair(employee.getJobHistory().get(0).getEmployer(), employee.getPerson()))
                .collect(groupingBy(Pair::getEmployer, mapping(Pair::getPerson, toSet())));


        assertThat(result, hasEntry(is("yandex"), contains(employees.get(2).getPerson())));
        assertThat(result, hasEntry(is("T-Systems"), containsInAnyOrder(employees.get(3).getPerson(), employees.get(5).getPerson())));
        assertThat(result, hasEntry(is("EPAM"), containsInAnyOrder(
                employees.get(0).getPerson(),
                employees.get(1).getPerson(),
                employees.get(4).getPerson()
        )));
    }

    /**
     * Преобразовать список сотрудников в отображение [компания -> сотрудник, суммарно проработавший в ней наибольшее время].
     * Гарантируется, что такой сотрудник будет один.
     */
    @Test
    void greatestExperiencePerEmployer() {
        List<Employee> employees = getEmployees();

        Map<String, Person> collect = employees.stream()
                .flatMap(employee -> employee.getJobHistory().stream()
                        .map(historyEntry ->
                                new Triple(historyEntry.getEmployer(),
                                        employee.getPerson(),
                                        historyEntry.getDuration())))
                .collect(toMap(t -> new Pair(t.getEmployer(), t.getPerson()),
                        Triple::getDuration, (t1, t2) -> t1 + t2))
                .entrySet().stream()
                .map(entry -> new Triple(entry.getKey().getEmployer(),
                                entry.getKey().getPerson(),
                                entry.getValue()))
                .collect(groupingBy(Triple::getEmployer, maxBy(Comparator.comparingInt(Triple::getDuration))))
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey,
                        entry -> entry.getValue().orElseThrow(IllegalAccessError::new).getPerson()));

        assertThat(collect, hasEntry("EPAM", employees.get(4).getPerson()));
        assertThat(collect, hasEntry("google", employees.get(1).getPerson()));
        assertThat(collect, hasEntry("yandex", employees.get(2).getPerson()));
        assertThat(collect, hasEntry("mail.ru", employees.get(2).getPerson()));
        assertThat(collect, hasEntry("T-Systems", employees.get(5).getPerson()));
    }


    @Value
    class Triple {
        String employer;
        Person person;
        int duration;
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
                                new JobHistoryEntry(2, "dev", "EPAM"),
                                new JobHistoryEntry(2, "dev", "google")
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