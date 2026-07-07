package dev.scx.exception;

/// ScxWrappedException 是一个通用的 异常包装器.
///
/// ### 主要用途 :
///
/// - 1. 将 受检异常 包装为 运行时异常, 以便在无法声明 throws 的场景中传播.
/// - 2. 在 方法 接收 外部逻辑 (如高阶函数) 时, 用于区分 方法自身的异常 与 外部逻辑抛出的异常 (异常域隔离).
///
/// ### 设计动机 :
///
/// 假设存在如下方法 :
///
/// ```java
/// public void read(Func<byte[]> bytesConsumer, int length) throws IOException {
///     // someCode
/// }
/// ```
///
/// 在调用 read 时, 我们往往难以区分异常的来源 (异常域模糊).
///
/// ```java
/// try {
///     read(bytes -> {
///         // someCode maybe throw IOException
///     }, 1024);
/// } catch (IOException e) {
///     // 这个 IOException 来自 read, 还是来自 bytesConsumer ?
///     e.printStackTrace();
/// }
/// ```
///
/// 针对这种情况, 我们创建 `ScxWrappedException` 用于解决 `异常域模糊` 的问题.
///
/// ### 使用方式 (唯一推荐) : 包装所有异常.
///
/// 统一使用 ScxWrappedException 对 外部逻辑 抛出的所有异常 进行包装, 以明确区分异常域.
///
/// 相应地, 调用方需要显式处理 ScxWrappedException, 并在需要时对其进行解包以获取真实异常.
///
/// 示例 :
///
/// ```java
/// public void read(Func<byte[]> bytesConsumer, int length) throws IOException, ScxWrappedException {
///     // someCode
///     try {
///         bytesConsumer.apply(new byte[]{1,2,3});
///     } catch (Throwable e) {
///         throw new ScxWrappedException(e);
///     }
///     // someCode
/// }
/// ```
/// #### 小提示 : 如果你的 `方法` 与 `外部逻辑` 之间, 不存在 异常域模糊 的可能, 那么你实际上并不需要这个类.
///
/// ### 反例 : 选择性包装
///
/// 我们可能很容易想到,
/// 既然 问题出在异常域模糊, 那么我们是否可以选择性包装.
/// 比如 只包装 会混淆的异常.
///
/// 示例 1 : `外部逻辑` 只抛出 RuntimeException
///
/// ```java
/// // 假设 NoPermissionException 是一个 RuntimeException.
/// public void checkPermission(Func<String[], RuntimeException> permissionsSupplier, String department) throws ScxWrappedException, NoPermissionException {
///     // someCode
///     try {
///         permissionsSupplier.apply();
///     } catch (NoPermissionException e) {
///         // 只包装会混淆的异常
///         throw new ScxWrappedException(e);
///     } catch (RuntimeException e) {
///         // 此处的 catch 代码块也可以直接删除, 此处为了演示.
///         throw e;
///     }
///     // someCode
/// }
/// ```
///
/// 示例 2 : `外部逻辑` 支持抛出 泛型异常 (包含受检异常).
///
/// ```java
/// public <X extends Throwable> void read(Func<byte[], X> bytesConsumer, int length) throws X, ScxWrappedException, IOException {
///     // someCode
///     try {
///         bytesConsumer.apply(new byte[]{1,2,3});
///     } catch (Throwable e) {
///         // 只包装会混淆的异常
///         if (e instanceof IOException) {
///             throw new ScxWrappedException(e);
///         }
///         // 其他异常直接抛出
///         throw e;
///     }
///     // someCode
/// }
/// ```
///
/// 然而, 一旦将视角提升到组合与抽象层面,
/// 选择性包装异常便无法形成稳定, 可组合的异常传播规则,
/// 因而在多层调用与框架级使用场景中必然退化.
///
/// 更具体地说, 该策略在设计层面存在以下结构性隐患 :
///
/// 1. 规则不稳定, 且无法通过类型系统表达, 调用方心智模型复杂.
///    比如 在同一个 API 中, 回调抛出的异常有时会被包装为 ScxWrappedException, 有时又会被直接透传,
///    调用方必须依赖实现细节或文档约定才能判断 "这个异常是否来自回调".
///    更常见的是 "在 回调内部 再次 调用其他回调" , 这种基于约定的判断会迅速失效, 异常来源将不再具备可机械推断性.
///
/// 2. 在链式组合 / 装饰器叠加场景下必然退化.
///    当该方法被再次包装, 组合或作为更高阶 API 的内部实现时, "哪些异常需要包装, 哪些可以透传" 的判定逻辑会被迫向上传染.
///    比如在回调存在多个, 且无共同自定义父类的异常类型时,
///    该设计要么导致异常签名持续膨胀, 要么不可避免地退化为捕获 Exception / Throwable,
///    从而使最初试图保留的异常精细语义完全丧失.
///
/// 3. 异常来源再次变得不可可靠区分, 破坏统一错误处理.
///    回调异常与宿主异常可能共享类型或继承体系, 选择性包装策略会重新引入 "异常来自哪里" 的歧义,
///    使日志归因, 监控分类, 等基于异常类型的统一处理 (例如 HTTP 状态码映射) 变得脆弱且高度依赖人为约定.
///
/// 因此, 尽管选择性包装在局部, 单层调用中看起来更 "简洁",
/// 但在任何需要长期演进, 复用与组合的抽象层中,
/// 该策略通常不会线性增加复杂度, 而是以隐蔽的方式积累风险, 并在后期集中爆发.
///
/// 需要强调的是, 这并非 "写法不当" 或 "使用不慎" 导致的风险, 而是选择性包装在设计层面上,
/// 无法形成稳定, 可组合的异常传播语义, 等同于引入一个会在未来必然触发的设计债, 因而必然失败.
///
/// @author scx567888
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

    /// 递归解包 ScxWrappedException 链, 返回第一个非 ScxWrappedException 的 cause
    public Throwable getUnwrappedCause() {
        var cause = this.getCause();
        if (cause instanceof ScxWrappedException wrappedException) {
            return wrappedException.getUnwrappedCause();
        }
        return cause;
    }

}
