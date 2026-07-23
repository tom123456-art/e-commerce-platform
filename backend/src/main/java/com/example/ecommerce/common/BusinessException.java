package com.example.ecommerce.common;

/**
 * 业务异常类
 * 运行时异常（非受检异常），不需要调用时强制try...catch或者throws
 * 编译异常
 */
public class BusinessException extends RuntimeException{
    /**业务状态码 需要与Result类中的常量对应起来*/
    private final int code;

    /**
     * 构造一个默认状态码为400的业务异常
     * */
    public BusinessException(String message) {
        //this(xx)执行当前类中的其它构造
        //Result.BAD_REQUEST_CODE = 400
        this(Result.BAD_REQUEST_CODE,message);
    }

    /**
     * 二个参数的构造方法：指定一个带状态码的业务异常
     * message 异常信息
     * code 状态码
     * */

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    //给私有属性添加get和set方法，code时final修饰的，不可以添加set方法修改code的值
    public int getCode() {
        return code;
    }
}
