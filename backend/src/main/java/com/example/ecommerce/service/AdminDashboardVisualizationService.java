package com.example.ecommerce.service;

import com.example.ecommerce.dto.AdminDashboardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * 后台看板可视化服务 —— 调用 Python PyEcharts 脚本生成图表 HTML。
 *
 * <h3>跨语言集成</h3>
 * Java 后端调用 Python 脚本生成图表，这在实际项目中很常见：
 * Python 的数据可视化生态（PyEcharts、Matplotlib）远比 Java 丰富。
 *
 * <h3>优雅降级（Graceful Degradation）</h3>
 * 当 Python 脚本不可用或执行失败时，返回友好的错误页面 HTML，
 * 而非抛出异常 —— 管理员后台看板即使图表渲染失败，也不应显示空白或报错。
 *
 * <h3>进程间通信（IPC）</h3>
 * - 通过 stdin（标准输入）将 JSON 数据传给 Python
 * - 通过 stdout（标准输出）接收生成的 HTML
 * - 设置 30 秒超时避免 Python 卡死阻塞 Java 线程
 * - finally 块清理临时文件，防止磁盘泄漏
 */
@Service
public class AdminDashboardVisualizationService {

    /** Python 脚本执行超时时间（秒） */
    private static final long SCRIPT_TIMEOUT_SECONDS = 30L;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 渲染后台看板 HTML。
     *
     * <p>业务流程：
     * <ol>
     *   <li>定位 Python 脚本（scripts/admin_dashboard_pyecharts.py）</li>
     *   <li>创建临时文件接收 stdout/stderr</li>
     *   <li>启动 Python 子进程，JSON 通过 stdin 传入</li>
     *   <li>等待进程完成（最多 30 秒）</li>
     *   <li>读取 stdout 中的 HTML</li>
     *   <li>校验 HTML 格式（必须包含 {@code <html} 标签）</li>
     *   <li>清理临时文件，返回 HTML</li>
     * </ol>
     * </p>
     *
     * @param response 看板数据
     * @return 可视化 HTML 字符串（失败时返回降级页面）
     */
    public String renderHtml(AdminDashboardResponse response) {
        Path scriptPath = Paths.get(System.getProperty("user.dir")).getParent().resolve("scripts").resolve("admin_dashboard_pyecharts.py");
        if (!Files.exists(scriptPath)) {
            return fallbackHtml("未找到 PyEcharts 渲染脚本：" + scriptPath);
        }

        Process process = null;
        Path stdoutPath = null;
        Path stderrPath = null;
        try {
            stdoutPath = Files.createTempFile("admin-dashboard-", ".html");
            stderrPath = Files.createTempFile("admin-dashboard-", ".log");

            // 启动 Python 子进程
            ProcessBuilder builder = new ProcessBuilder(resolvePythonCommand(), scriptPath.toString());
            builder.environment().put("PYTHONIOENCODING", "UTF-8");  // 强制 UTF-8 输出
            builder.environment().put("PYTHONUTF8", "1");
            builder.redirectOutput(stdoutPath.toFile());
            builder.redirectError(stderrPath.toFile());
            process = builder.start();

            // 通过 stdin 传入 JSON 数据
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsBytes(response));
                outputStream.flush();
            }

            // 等待进程完成（带超时）
            boolean finished = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String stdout = readFile(stdoutPath);
            String stderr = readFile(stderrPath);

            if (!finished) {
                process.destroyForcibly();
                return fallbackHtml("PyEcharts 渲染超时，请检查 Python 环境或图表脚本。");
            }

            if (process.exitValue() != 0) {
                return fallbackHtml("PyEcharts 渲染失败：" + safeMessage(stderr.isEmpty() ? stdout : stderr));
            }

            if (!stdout.contains("<html")) {
                return fallbackHtml("PyEcharts 输出格式异常：" + safeMessage(stdout));
            }

            return stdout;
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            return fallbackHtml("PyEcharts 渲染异常：" + safeMessage(ex.getMessage()));
        } finally {
            // finally 块确保临时文件被清理
            deleteIfExists(stdoutPath);
            deleteIfExists(stderrPath);
        }
    }

    /**
     * 解析 Python 命令路径。
     * 优先使用环境变量 PYTHON_COMMAND，未配置则用 "python"。
     * 这样部署时可灵活指定虚拟环境中的 Python。
     */
    private String resolvePythonCommand() {
        String configured = System.getenv("PYTHON_COMMAND");
        return configured == null || configured.trim().isEmpty() ? "python" : configured.trim();
    }

    private String readFile(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    /**
     * 生成降级 HTML 页面。
     * <p>使用内联 CSS 样式确保页面美观，对错误信息进行 HTML 转义防 XSS。</p>
     */
    private String fallbackHtml(String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>后台看板</title>"
            + "<style>body{font-family:'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;background:#eef3f9;padding:24px;color:#152033;}"
            + ".card{max-width:900px;margin:0 auto;background:#fff;border-radius:20px;padding:28px;box-shadow:0 18px 40px rgba(15,23,42,.08);}"
            + "h1{margin:0 0 12px;font-size:28px;}p{line-height:1.8;color:#5f6b7c;}"
            + "code{display:block;margin-top:16px;padding:14px;border-radius:14px;background:#f8fbff;color:#c2410c;white-space:pre-wrap;}</style>"
            + "</head><body><div class=\"card\"><h1>PyEcharts 看板暂不可用</h1>"
            + "<p>图表渲染失败，系统已回退到说明页。</p><code>"
            + escapeHtml(message)
            + "</code></div></body></html>";
    }

    private String safeMessage(String message) {
        return message == null || message.trim().isEmpty() ? "未知错误" : message.trim();
    }

    /**
     * HTML 转义 —— 防止 XSS 攻击。
     * 将用户可控内容嵌入 HTML 时必须转义。
     */
    private String escapeHtml(String value) {
        return safeMessage(value)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
