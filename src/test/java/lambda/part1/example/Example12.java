package lambda.part1.example;

public class Example12 {

    private int value = 55;

    public static void main(String[] args) {
        int value = 66;
//        System.out.println(this.value);
    }

    public void method() {
        int value = 66;

        Runnable task = () -> System.out.println(this.value);

    }
}
