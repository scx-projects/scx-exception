package dev.scx.exception;

/// ScxWrappedException 是一个通用的运行时异常包装器, 主要用途 :
///
/// - 1. 将受检异常包装为运行时异常.
/// - 2. 对动态可执行单元 (如高阶函数, 装饰器模式) 进行异常隔离和标记.
///
/// ### 设计动机
///
/// 设想一下有如下方法
/// ```java
/// public void read(Func<byte[]> bytesConsumer, int length) throws IOException {
///     // someCode
/// }
/// ```
/// 当我们在调用时就会遇到如下问题, 也就是异常来源的模糊性问题.
/// ```java
/// try {
///     read(bytes -> {
///         // someCode maybe throw IOException
///     }, 1024);
/// } catch (IOException e) {
///     // 该如何区分这个 IOException 是 read 本身的 还是 bytesConsumer 的 ?
///     e.printStackTrace();
/// }
/// ```
///
/// 针对这种情况 我们创建 `ScxWrappedException` 用于包装 `bytesConsumer` 的异常, 以便调用者能够正确区分.
///
/// ### 用法展示, 这里我展示 `两种推荐用法` .
///
/// #### 用法 1 : 包装所有异常.
///
/// 案例 1 :
/// ```java
/// public void read(Func<byte[]> bytesConsumer, int length) throws ScxWrappedException, IOException {
///     // someCode
///     try {
///         bytesConsumer.apply(new byte[]{1,2,3});
///     } catch (Exception e) {
///         throw new ScxWrappedException(e);
///     }
///     // someCode
/// }
/// ```
///
/// - 优点 : 异常隔离层级清晰, 调用方心智负担小.
/// - 缺点 : 对于调用方 总是需要处理 ScxWrappedException 异常.
///
/// #### 用法 2 : 只包装可能引发混淆的异常.
///
/// 案例 1 : `动态可执行单元` 只抛出 RuntimeException.
///
/// ```java
/// // 假设 NoPermissionException 是一个 RuntimeException.
/// public void checkPermission(Func<String[], RuntimeException> permissionsSupplier, String department) throws ScxWrappedException, NoPermissionException {
///     // someCode
///     try {
///         permissionsSupplier.apply();
///     } catch (NoPermissionException e) {
///         // 只包装可能混淆的异常
///         throw new ScxWrappedException(e);
///     } catch (RuntimeException e) {
///         // 此处的 catch 代码块也可以直接删除, 此处为了演示.
///         throw e;
///     }
///     // someCode
/// }
/// ```
///
/// 案例 2 : `动态可执行单元` 支持抛出 泛型异常 (包含受检异常).
///
/// ```java
/// public <X extends Exception> void read(Func<byte[], X> bytesConsumer, int length) throws X, ScxWrappedException, IOException {
///     // someCode
///     try {
///         bytesConsumer.apply(new byte[]{1,2,3});
///     } catch (Exception e) {
///         // 包装易混淆异常
///         if(e instanceof IOException) {
///             throw new ScxWrappedException(e);
///         }
///         // 其他异常直接抛出
///         throw e;
///     }
///     // someCode
/// }
/// ```
///
/// - 优点 : 对于不会混淆的异常 会直接穿透到调用者. 在异常层级上会更干净, 而且更符合所谓的函数式哲学.
/// - 缺点 : 模糊了异常的层级, 对调用者来说 有些异常会包装有些不会包装, 可能造成一定程度的心智负担.
/// - 小提示 1 : 在案例 2 中, 实际使用过程中 X 会被推断为 bytesConsumer 抛出的所有异常的最小共同父类.
/// - 小提示 2 : 如果你的 `动态可执行单元` 永远不会抛出令人混淆的异常, 那么你实际上不需要这个类.
///
/// @author scx567888
/// @version 0.0.1
public final class ScxWrappedException extends RuntimeException {

    public ScxWrappedException(String message, Throwable cause) {
        // 包装类 不允许空 cause
        if (cause == null) {
            throw new NullPointerException("cause must not be null");
        }
        super(message, cause);
    }

    public ScxWrappedException(Throwable cause) {
        // 包装类 不允许空 cause
        if (cause == null) {
            throw new NullPointerException("cause must not be null");
        }
        super(cause);
    }

    /// 获取真正的异常
    public Throwable getRootCause() {
        var cause = this.getCause();
        if (cause instanceof ScxWrappedException wrappedException) {
            return wrappedException.getRootCause();
        }
        return cause;
    }

}
