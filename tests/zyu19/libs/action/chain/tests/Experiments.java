package zyu19.libs.action.chain.tests;

import zyu19.libs.action.chain.ExceptionList;

/**
 * Created by zyu on 3/15/16.
 */
public class Experiments {
    public static void main(String[] args) {
        Exception err = (Exception)(Object) new String("abc");
        System.out.println("No complain?");
        return ;
    }
}
