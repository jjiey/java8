package demo.test;

import com.sun.corba.se.impl.orbutil.DenseIntMapImpl;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DenseIntMapImplTest {

    public static void main(String[] args) {
        DenseIntMapImplTest test = new DenseIntMapImplTest();
        test.test();
    }

    private void test() {
        DenseIntMapImpl map = new DenseIntMapImpl();
        map.set(9, "999");
        map.set(8, "888");
        map.set(13, "888");
        System.out.println(map.get(9));

        List<Testdto> ttt = new ArrayList<>();
        Testdto t1 = new Testdto();
        t1.setId(1);
        t1.setName("1");
        Testdto t2 = new Testdto();
        t2.setId(1);
        t2.setName("2");
        ttt.add(t1);
        ttt.add(t2);
        System.out.println(ttt.stream().collect(Collectors.toMap(Testdto::getId, Testdto::getName, (oldV, newV) -> newV)));
    }

    @Data
    class Testdto {

        private int id;

        private String name;

        private boolean t;
    }
}
