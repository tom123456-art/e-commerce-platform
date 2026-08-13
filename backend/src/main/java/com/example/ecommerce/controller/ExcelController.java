package com.example.ecommerce.controller;

import com.example.ecommerce.entity.Order;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.utils.ExcelUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Excel导入导出接口",description = "商品数据Excel导入导出")
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private final OrderService orderService;

    public ExcelController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 导出订单信息
     * @param response
     * @throws IOException
     */
    @GetMapping("/exportOrders")
    public void exportOrders(HttpServletResponse response) throws IOException {
        // 1、查询订单数据
        List<Order> orders = orderService.getAll();
        // 2、定义Excel表头
        String[] headers = {
               "订单ID","用户ID", "订单编号", "总金额",
                "状态", "地址", "电话", "收货人", "创建时间"
        };
        // 3、Order实体类映射为List<Map>结构
        List<Map<String, Object>> dataList = new ArrayList<>();
        // 遍历订单
        for (Order order: orders){
            Map<String, Object> row = new HashMap<>();
            row.put("订单ID", order.getId());
            row.put("用户ID", order.getUserId());
            row.put("订单编号", order.getOrderNo());
            row.put("总金额", order.getTotalAmount());
            row.put("状态", getStatusLabel(order.getStatus()));
            row.put("地址", order.getAddress());
            row.put("电话", order.getPhone());
            row.put("收货人", order.getReceiver());
            row.put("创建时间", order.getCreateTime());
            dataList.add(row);
        }
        // 4、生成Excel字节数组
        byte[] excelBytes = ExcelUtil.exportExcel(headers, dataList);
        // 5、设置响应头，告诉浏览器这是一个需要下载的文件
        response.setHeader("Content-Disposition", "attachment;filename=orders.xlsx");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setContentLength(excelBytes.length);
        // 6、写入响应输出流
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "未知";
        switch (status){
            case 0: return "待支付";
            case 1: return "已支付";
            case 2: return "已完成";
            default: return "未知";
        }
    }

    /**
     * 下载商品批量导入的Excel模板
     * @param response
     */
    @GetMapping("/productImportTemplate")
    public void downloadProductImportTemplate(HttpServletResponse response) throws IOException {
        byte[] excelBytes = ExcelUtil.exportProductImportTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setContentLength(excelBytes.length);
        response.setHeader("Content-Disposition", "attachment;filename=product_import_template.xlsx");
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }
}




