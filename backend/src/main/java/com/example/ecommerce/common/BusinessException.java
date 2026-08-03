package com.example.ecommerce.common;

/**
 * 业务异常类
 *    运行时异常(非受检异常)，不需要调用时强制try...catch或者throws
 *    编译异常
 */
public class BusinessException extends RuntimeException{
    /** 业务状态码 需要与Result类中的常量对应起来
     *     public static final int SUCCESS_CODE = 200;      // 成功
     *     public static final int BAD_REQUEST_CODE = 400; // 参数错误
     *     public static final int UNAUTHORIZED_CODE = 401;// 未登录
     *     public static final int FORBIDDEN_CODE = 403;   // 无权限
     *     public static final int NOT_FOUND_CODE = 404;   // 资源不存在
     *     public static final int CONFLICT_CODE = 409;     // 数据冲突
     *     public static final int ERROR_CODE = 500;       // 服务器错误
     * */
    private final int code;

    /**
     * 构造一个默认状态码为400的业务异常
     * @param message   异常信息
     * 使用场景： new BusinessException("用户名已经存在")
     */
    public BusinessException(String message) {
        //this(xx)执行当前类中的其它构造
        //Result.BAD_REQUEST_CODE  =  400
        this(Result.BAD_REQUEST_CODE,message);
    }

    /**
     * 二个参数的构造方法：指定一个带状态码的业务异常
     * @param message  异常信息
     * @param code     状态码  200  400  401....
     * 使用场景演示：  new BusinessException(404, "商品不存在")
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    //给私有属性添加get和set方法  code是final修饰的，不可以添加set方法修改code的值
    public int getCode() {
        return code;
    }
}
