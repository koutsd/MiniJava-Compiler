class test {
    public static void main(String[] args) {
        B b;
        b = new B();
    }
}


class A {
    int a;
    int b;

    public int get6(int a){
        int s;
        int f;

        s = f + 1;
        f = s + 2;
        return 1;
    }
    public int get1234(int a){
        return 1;
    }
}

class B extends  A{
    int a;
    int b;
    public int get(int a){

        return 1;
    }
    public int get1(int a){

        return 1;
    }
    public int get1234(int a){

        return 1;
    }

}

class C extends  B{
    int a;
    int b;
    public int get10(int a){

        return 1;
    }
    public int get1234(int a){
        int s;
        return 1;
    }
    public int get6(int a){
        int s;
        s = a + 3;
        return 1;
    }

}