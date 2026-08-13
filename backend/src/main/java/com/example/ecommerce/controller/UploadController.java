package com.example.ecommerce.controller;

import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件上传控制器 —— 处理图片上传与已上传图片列表查询。
 *
 * 【安全设计：四层防御】
 *   1. 空文件检查：file.isEmpty() 拒绝空上传
 *   2. MIME 类型白名单：contentType.startsWith("image/") 只允许图片
 *   3. 文件大小限制：5MB 上限，防止恶意大文件耗尽磁盘
 *   4. UUID 重命名：不用客户端原始文件名，防止目录遍历攻击和文件名冲突
 */
@Tag(name = "文件上传接口", description = "图片等文件上传")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    /** 上传目录（可配置）：${app.upload.dir:uploads} 表示读配置，缺省值为 "uploads" */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** 图片访问 URL 前缀：拼接在文件名前构成完整访问地址 */
    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /** 允许的图片扩展名白名单（小写，含前导 "."） */
    private static final java.util.Set<String> ALLOWED_IMAGE_EXTENSIONS = java.util.Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg"
    );

    /**
     * 上传图片接口。
     *
     * 请求：POST /api/upload/image
     * Content-Type: multipart/form-data
     * 表单字段：file（文件）
     *
     * 响应：Result<Map<String,String>>，包含 url 和 filename
     */
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {

        // ===== 第 1 层防御：空文件检查 =====
        if (file.isEmpty()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "请选择要上传的文件");
        }

        // ===== 第 2 层防御：MIME 类型白名单 =====
        // 注意：MIME 由客户端声明，理论上可伪造。生产环境还应检查文件头魔数（Magic Number）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "只能上传图片文件");
        }

        // ===== 第 3 层防御：文件大小限制（5MB） =====
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "图片大小不能超过5MB");
        }

        try {
            // 创建上传目录（如果不存在）
            // Files.createDirectories() 类似 mkdir -p，递归创建
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // ===== 第 4 层防御：UUID 重命名 =====
            // 不用原始文件名，原因：
            //   1. 防目录遍历：攻击者上传 "../../etc/cron.d/evil" 会被 UUID 替换
            //   2. 防冲突：不同用户都传 "photo.jpg" 不会互相覆盖
            //   3. 防编码问题：中文文件名在不同 OS 编码不一致
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                // 取最后一个 "." 之后的扩展名（含 "."）
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // 扩展名白名单校验（防止 .jsp/.php 等可执行文件伪装成图片）
            String extLower = extension.toLowerCase(java.util.Locale.ROOT);
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extLower)) {
                return Result.failure(400, "不支持的文件类型，仅允许: " + ALLOWED_IMAGE_EXTENSIONS);
            }
            String filename = UUID.randomUUID().toString() + extLower;

            // 保存文件到磁盘
            // resolve() 拼接路径，transferTo() 将临时文件移动到目标位置
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // 构造返回结果：URL = urlPrefix + "/" + filename
            // 例如 "/uploads/abc-123.jpg"，前端可直接用于 <img :src="url">
            String url = urlPrefix + "/" + filename;
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", filename);
            return Result.success(result);

        } catch (IOException e) {
            // 文件保存失败：磁盘满、无写权限、路径过长等
            throw new BusinessException(Result.ERROR_CODE, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取已上传图片列表（用于图片选择器弹窗）。
     *
     * 请求：GET /api/upload/images?dir=&urlPrefixParam=
     * 响应：Result<List<Map>>，每项包含 filename、url、size
     *
     * 【路径安全】checkPathSafety 防止目录遍历：扫描路径必须在 uploadDir 根目录内
     */
    @GetMapping("/images")
    public Result<List<Map<String, String>>> listImages(
            @RequestParam(required = false) String dir,
            @RequestParam(required = false) String urlPrefixParam) {

        String scanDir = (dir != null && !dir.isEmpty()) ? dir : uploadDir;
        String prefix = (urlPrefixParam != null && !urlPrefixParam.isEmpty()) ? urlPrefixParam : urlPrefix;

        // 路径遍历防护：normalize() 后比较前缀，确保扫描路径在 uploadRoot 内
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path scanPath = Paths.get(scanDir).toAbsolutePath().normalize();
        if (!scanPath.startsWith(uploadRoot)) {
            return Result.failure(Result.FORBIDDEN_CODE, "Access denied: directory outside uploads root");
        }

        if (!Files.exists(scanPath) || !Files.isDirectory(scanPath)) {
            return Result.success(new ArrayList<>());
        }

        // 递归收集图片文件
        List<Map<String, String>> images = new ArrayList<>();
        collectImages(scanPath, scanPath, prefix, images);
        return Result.success(images);
    }

    /**
     * 递归收集目录中的图片文件信息（深度优先遍历）。
     *
     * DirectoryStream 比 File.listFiles() 更节省内存（延迟加载），
     * 适合处理包含大量文件的目录。
     */
    private void collectImages(Path root, Path dir, String prefix, List<Map<String, String>> images) {
        // 第 1 步：扫描当前目录中的图片文件（Glob 过滤器只匹配图片扩展名）
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jpg,jpeg,png,gif,webp,bmp,svg}")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    Map<String, String> info = new HashMap<>();
                    String filename = file.getFileName().toString();
                    // relativize() 计算相对路径，replace("\\", "/") 统一为 URL 格式
                    String relativePath = root.relativize(file).toString().replace("\\", "/");
                    info.put("filename", filename);
                    info.put("url", prefix + "/" + relativePath);
                    info.put("size", String.valueOf(Files.size(file)));
                    images.add(info);
                }
            }
        } catch (IOException e) {
            // 静默处理：某个目录无权限时跳过，不影响整体结果
        }

        // 第 2 步：递归扫描子目录
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    collectImages(root, entry, prefix, images);
                }
            }
        } catch (IOException e) {
            // 同样静默处理
        }
    }
}
