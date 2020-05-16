class ManyClasses {
    public static void main(String[] x) {
        boolean rv;
        A a;
        B b;
        b = new B();
        rv = b.set();
        System.out.println(b.get());
    }
}

class A {
    boolean data;

    public int get(){
        int rv;
        if(data){
            rv = 1;
        }
        else{
            rv = 0;
        }
        return rv;
    }
}

class B extends A {
    public boolean set() {
        boolean old;
        data = false;
        old = data;
        data = true;
        return data;
    }
}
