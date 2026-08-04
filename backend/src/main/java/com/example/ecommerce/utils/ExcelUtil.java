package com.example.ecommerce.utils;

import com.example.ecommerce.entity.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Excel 导入导出工具类 —— 基于 Apache POI 实现。
 *
 * 设计原则：
 *   1. 纯静态方法：工具类无状态，所有方法 static
 *   2. 通用导出 + 专用导入：exportExcel 是通用方法，parseProducts 是商品专用解析
 *   3. 容错处理：空行跳过、类型转换失败时抛出带行号的错误信息
 *
 * 使用场景：
 *   - 管理后台"导出商品列表"按钮 → exportExcel()
 *   - 管理后台"下载导入模板"按钮 → exportProductImportTemplate()
 *   - 管理后台"上传 Excel 导入商品"按钮 → parseProducts()
 *
 * 依赖：pom.xml 中需要引入 poi 和 poi-ooxml（第09章管理后台引入）
 */
public class ExcelUtil {

    /** 商品导入模板的列头（中文） */
    private static final String[] PRODUCT_HEADERS = {
        "商品名称", "商品描述", "价格", "库存", "图片地址", "分类ID", "上架状态"
    };

    /**
     * 列头映射：支持中文和英文列头（用户上传的 Excel 可能用任一语言）。
     * Key = 规范化后的列头（去空格、转小写），Value = 标准字段名
     */
    private static final Map<String, String> PRODUCT_HEADER_MAPPING = new LinkedHashMap<>();
    static {
        PRODUCT_HEADER_MAPPING.put("商品名称", "name");
        PRODUCT_HEADER_MAPPING.put("name", "name");
        PRODUCT_HEADER_MAPPING.put("商品描述", "description");
        PRODUCT_HEADER_MAPPING.put("description", "description");
        PRODUCT_HEADER_MAPPING.put("价格", "price");
        PRODUCT_HEADER_MAPPING.put("price", "price");
        PRODUCT_HEADER_MAPPING.put("库存", "stock");
        PRODUCT_HEADER_MAPPING.put("stock", "stock");
        PRODUCT_HEADER_MAPPING.put("图片地址", "image");
        PRODUCT_HEADER_MAPPING.put("image", "image");
        PRODUCT_HEADER_MAPPING.put("分类id", "categoryId");
        PRODUCT_HEADER_MAPPING.put("categoryid", "categoryId");
        PRODUCT_HEADER_MAPPING.put("上架状态", "status");
        PRODUCT_HEADER_MAPPING.put("status", "status");
    }

    /**
     * 通用 Excel 导出：将数据列表导出为 .xlsx 格式的字节数组。
     *
     * @param headers  列头数组（如 {"商品名称", "价格", "库存"}）
     * @param dataList 数据列表，每个 Map 的 Key 对应一列数据
     * @return Excel 文件的字节数组（可直接写入 HttpServletResponse 输出流）
     */
    public static byte[] exportExcel(String[] headers, List<Map<String, Object>> dataList) throws IOException {
        // 创建工作簿（.xlsx 格式）
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("数据");

            // 创建表头样式：加粗 + 灰色背景
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 写入表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行
            for (int rowIdx = 0; rowIdx < dataList.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                Map<String, Object> data = dataList.get(rowIdx);
                for (int colIdx = 0; colIdx < headers.length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object value = data.get(headers[colIdx]);
                    cell.setCellValue(value == null ? "" : String.valueOf(value));
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 生成商品导入模板（含一行示例数据）。
     * 用户下载模板后按格式填写，再上传导入。
     *
     * @return Excel 模板文件的字节数组
     */
    public static byte[] exportProductImportTemplate() throws IOException {
        List<Map<String, Object>> sampleData = new ArrayList<>();
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("商品名称", "示例商品");
        sample.put("商品描述", "这是一个示例商品描述");
        sample.put("价格", "99.99");
        sample.put("库存", "100");
        sample.put("图片地址", "/images/products/sample.jpg");
        sample.put("分类ID", "1");
        sample.put("上架状态", "1");
        sampleData.add(sample);
        return exportExcel(PRODUCT_HEADERS, sampleData);
    }

    /**
     * 解析上传的 Excel 文件为商品列表。
     *
     * 处理流程：
     *   1. 读取表头行，建立"列索引 → 字段名"的映射
     *   2. 校验必填列是否存在（商品名称、价格）
     *   3. 逐行读取数据，跳过空行
     *   4. 类型转换：价格 → BigDecimal，库存/分类 → Integer，状态 → 1/0
     *   5. 转换失败时抛出带行号的错误信息，方便用户定位问题
     *
     * @param inputStream 上传的 Excel 文件输入流
     * @return 解析后的商品列表
     * @throws IllegalArgumentException 格式错误时抛出（含行号提示）
     */
    public static List<Product> parseProducts(InputStream inputStream) throws IOException {
        List<Product> products = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("Excel 文件为空或缺少数据行");
            }

            // 第一步：读取表头，建立列索引映射
            Row headerRow = sheet.getRow(0);
            Map<Integer, String> columnIndexMap = readHeaderIndexMap(headerRow);
            validateProductHeaders(columnIndexMap);

            // 第二步：逐行解析数据
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isEmptyRow(row)) {
                    continue;  // 跳过空行
                }

                Product product = new Product();
                // 商品名称（必填）
                String name = readRequiredCell(row, columnIndexMap, "name", rowIdx);
                product.setName(name);
                // 商品描述（可选）
                product.setDescription(readOptionalCell(row, columnIndexMap, "description"));
                // 价格（必填，BigDecimal）
                String priceStr = readRequiredCell(row, columnIndexMap, "price", rowIdx);
                product.setPrice(parseDecimal(priceStr, rowIdx, "价格"));
                // 库存（可选，默认 0）
                String stockStr = readOptionalCell(row, columnIndexMap, "stock");
                product.setStock(stockStr == null ? 0 : parseInteger(stockStr, rowIdx, "库存"));
                // 图片地址（可选）
                product.setImage(readOptionalCell(row, columnIndexMap, "image"));
                // 分类 ID（可选）
                String categoryStr = readOptionalCell(row, columnIndexMap, "categoryId");
                product.setCategoryId(categoryStr == null ? null : parseInteger(categoryStr, rowIdx, "分类ID"));
                // 上架状态（可选，默认上架）
                String statusStr = readOptionalCell(row, columnIndexMap, "status");
                product.setStatus(statusStr == null ? 1 : parseStatus(statusStr, rowIdx));

                products.add(product);
            }
        }

        if (products.isEmpty()) {
            throw new IllegalArgumentException("Excel 中没有可导入的商品数据");
        }
        return products;
    }

    // ==================== 内部辅助方法 ====================

    /** 读取表头行，返回"列索引 → 标准字段名"的映射 */
    private static Map<Integer, String> readHeaderIndexMap(Row headerRow) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String header = normalizeHeader(cell.getStringCellValue());
            String fieldName = PRODUCT_HEADER_MAPPING.get(header);
            if (fieldName != null) {
                map.put(i, fieldName);
            }
        }
        return map;
    }

    /** 校验必填列是否存在 */
    private static void validateProductHeaders(Map<Integer, String> columnIndexMap) {
        Set<String> fields = new HashSet<>(columnIndexMap.values());
        if (!fields.contains("name")) {
            throw new IllegalArgumentException("Excel 缺少必填列：商品名称");
        }
        if (!fields.contains("price")) {
            throw new IllegalArgumentException("Excel 缺少必填列：价格");
        }
    }

    /** 读取必填单元格，为空时抛出带行号的错误 */
    private static String readRequiredCell(Row row, Map<Integer, String> map, String field, int rowIdx) {
        String value = readCellByField(row, map, field);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("第 " + (rowIdx + 1) + " 行缺少必填字段：" + field);
        }
        return value.trim();
    }

    /** 读取可选单元格，为空时返回 null */
    private static String readOptionalCell(Row row, Map<Integer, String> map, String field) {
        String value = readCellByField(row, map, field);
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /** 根据字段名找到对应列索引，读取单元格值 */
    private static String readCellByField(Row row, Map<Integer, String> map, String field) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue().equals(field)) {
                Cell cell = row.getCell(entry.getKey());
                if (cell == null) return null;
                // 处理公式单元格
                if (cell.getCellType() == CellType.FORMULA) {
                    return String.valueOf(cell.getNumericCellValue());
                }
                cell.setCellType(CellType.STRING);
                return cell.getStringCellValue();
            }
        }
        return null;
    }

    /** 解析 BigDecimal，失败时抛出带行号的错误 */
    private static BigDecimal parseDecimal(String value, int rowIdx, String fieldName) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第 " + (rowIdx + 1) + " 行" + fieldName + "格式错误：" + value);
        }
    }

    /** 解析 Integer，失败时抛出带行号的错误 */
    private static Integer parseInteger(String value, int rowIdx, String fieldName) {
        try {
            return Integer.parseInt(value.contains(".") ? value.substring(0, value.indexOf('.')) : value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第 " + (rowIdx + 1) + " 行" + fieldName + "格式错误：" + value);
        }
    }

    /** 解析上架状态：支持 1/0、true/false、上架/下架 */
    private static Integer parseStatus(String value, int rowIdx) {
        String normalized = value.trim().toLowerCase();
        if ("1".equals(normalized) || "true".equals(normalized) || "上架".equals(normalized)) {
            return 1;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "下架".equals(normalized)) {
            return 0;
        }
        throw new IllegalArgumentException("第 " + (rowIdx + 1) + " 行上架状态无法识别：" + value + "（支持 1/0、上架/下架）");
    }

    /** 判断是否为空行（所有单元格都为空） */
    private static boolean isEmptyRow(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = cell.toString().trim();
                if (!value.isEmpty()) return false;
            }
        }
        return true;
    }

    /** 规范化列头：去空格、转小写，支持灵活匹配 */
    private static String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase().replaceAll("\\s+", "");
    }
}
