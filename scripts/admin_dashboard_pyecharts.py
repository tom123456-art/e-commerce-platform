import json
import sys

from pyecharts import options as opts
from pyecharts.charts import Bar, Funnel, Page, Pie
from pyecharts.components import Table
from pyecharts.globals import ThemeType


def safe_items(data, key):
    value = data.get(key) or []
    return value if isinstance(value, list) else []


def build_table(data):
    table = Table()
    headers = ["指标", "数值"]
    rows = [
        ["用户总数", data.get("userCount", 0)],
        ["商品总数", data.get("productCount", 0)],
        ["订单总数", data.get("orderCount", 0)],
        ["待支付订单", data.get("pendingOrderCount", 0)],
        ["已支付订单", data.get("paidOrderCount", 0)],
        ["已完成订单", data.get("completedOrderCount", 0)],
        ["低库存商品", data.get("lowStockProductCount", 0)],
    ]
    table.add(headers, rows)
    table.set_global_opts(
        title_opts=opts.ComponentTitleOpts(
            title="运营总览",
            subtitle="Spring Boot 聚合数据 · PyEcharts 可视化",
        )
    )
    return table


def build_order_pie(data):
    items = safe_items(data, "orderStatusDistribution")
    pairs = [(item.get("label", "未知"), float(item.get("value", 0))) for item in items]
    return (
        Pie(init_opts=opts.InitOpts(width="540px", height="360px", theme=ThemeType.LIGHT))
        .add(
            series_name="订单状态",
            data_pair=pairs,
            radius=["38%", "65%"],
        )
        .set_global_opts(
            title_opts=opts.TitleOpts(title="订单状态分布", pos_left="center"),
            legend_opts=opts.LegendOpts(pos_left="left", pos_top="18%", orient="vertical"),
        )
        .set_series_opts(label_opts=opts.LabelOpts(formatter="{b}: {c}"))
    )


def build_category_bar(data):
    items = safe_items(data, "productCategoryDistribution")
    labels = [item.get("label", "未知") for item in items]
    values = [round(float(item.get("value", 0)), 2) for item in items]
    return (
        Bar(init_opts=opts.InitOpts(width="720px", height="360px", theme=ThemeType.LIGHT))
        .add_xaxis(labels)
        .add_yaxis("商品数", values, category_gap="45%")
        .set_global_opts(
            title_opts=opts.TitleOpts(title="商品分类分布", pos_left="center"),
            legend_opts=opts.LegendOpts(pos_top="8%"),
            xaxis_opts=opts.AxisOpts(axislabel_opts=opts.LabelOpts(rotate=-20)),
            yaxis_opts=opts.AxisOpts(name="商品数"),
        )
    )


def build_hot_funnel(data):
    items = safe_items(data, "hotProductDistribution")
    pairs = [
        (item.get("label", "未知"), round(float(item.get("value", 0)) * 100, 2))
        for item in items
    ]
    return (
        Funnel(init_opts=opts.InitOpts(width="540px", height="360px", theme=ThemeType.LIGHT))
        .add("热卖分值", pairs, sort_="descending", gap=4)
        .set_global_opts(
            title_opts=opts.TitleOpts(title="热卖商品榜", pos_left="center"),
            legend_opts=opts.LegendOpts(pos_top="8%"),
        )
        .set_series_opts(label_opts=opts.LabelOpts(position="inside", formatter="{b}"))
    )


def build_recommend_bar(data):
    items = safe_items(data, "recommendedProductDistribution")
    labels = [item.get("label", "未知") for item in items]
    values = [round(float(item.get("value", 0)) * 100, 2) for item in items]
    return (
        Bar(init_opts=opts.InitOpts(width="720px", height="360px", theme=ThemeType.LIGHT))
        .add_xaxis(labels)
        .add_yaxis("推荐分", values, category_gap="45%")
        .set_global_opts(
            title_opts=opts.TitleOpts(title="首页推荐榜", pos_left="center"),
            legend_opts=opts.LegendOpts(pos_top="8%"),
            xaxis_opts=opts.AxisOpts(axislabel_opts=opts.LabelOpts(rotate=-20)),
            yaxis_opts=opts.AxisOpts(name="推荐分（%）"),
        )
    )


def build_weight_pie(data):
    items = safe_items(data, "recommendationWeightDistribution")
    pairs = [
        (item.get("label", "未知"), round(float(item.get("value", 0)) * 100, 2))
        for item in items
    ]
    return (
        Pie(init_opts=opts.InitOpts(width="540px", height="360px", theme=ThemeType.LIGHT))
        .add(
            series_name="推荐权重",
            data_pair=pairs,
            radius=["35%", "65%"],
        )
        .set_global_opts(
            title_opts=opts.TitleOpts(title="推荐权重配置", pos_left="center"),
            legend_opts=opts.LegendOpts(pos_left="left", pos_top="18%", orient="vertical"),
        )
        .set_series_opts(label_opts=opts.LabelOpts(formatter="{b}: {c}%"))
    )


def build_user_bar(data):
    role_items = safe_items(data, "userRoleDistribution")
    status_items = safe_items(data, "userStatusDistribution")

    labels = [item.get("label", "未知") for item in role_items] + [
        item.get("label", "未知") for item in status_items
    ]
    values = [round(float(item.get("value", 0)), 2) for item in role_items] + [
        round(float(item.get("value", 0)), 2) for item in status_items
    ]

    return (
        Bar(init_opts=opts.InitOpts(width="720px", height="360px", theme=ThemeType.LIGHT))
        .add_xaxis(labels)
        .add_yaxis("人数", values, category_gap="50%")
        .set_global_opts(
            title_opts=opts.TitleOpts(title="用户结构分布", pos_left="center"),
            legend_opts=opts.LegendOpts(pos_top="8%"),
            xaxis_opts=opts.AxisOpts(axislabel_opts=opts.LabelOpts(rotate=-20)),
            yaxis_opts=opts.AxisOpts(name="人数"),
        )
    )


def render_page(data):
    page = Page(layout=Page.SimplePageLayout)
    page.add(
        build_table(data),
        build_order_pie(data),
        build_category_bar(data),
        build_hot_funnel(data),
        build_recommend_bar(data),
        build_weight_pie(data),
        build_user_bar(data),
    )

    html = page.render_embed()
    html = html.replace("<title>Awesome-pyecharts</title>", "<title>运营看板</title>")
    style = """
    <style>
      body {
        margin: 0;
        padding: 18px;
        background: linear-gradient(180deg, #f7faff 0%, #eef3f9 100%);
        font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
        color: #152033;
      }
      .box {
        border-radius: 20px !important;
        box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08) !important;
        border: 1px solid rgba(148, 163, 184, 0.14) !important;
        background: rgba(255, 255, 255, 0.96) !important;
        padding: 12px !important;
        margin-bottom: 18px !important;
      }
      .box h2, .box h3 {
        color: #152033 !important;
      }
      .chart-container {
        border-radius: 18px;
        overflow: hidden;
      }
    </style>
    """
    return html.replace("</head>", style + "\n</head>")


def main():
    raw_bytes = sys.stdin.buffer.read()
    raw = raw_bytes.decode("utf-8", errors="replace").strip()
    data = json.loads(raw) if raw else {}
    html = render_page(data)
    sys.stdout.buffer.write(html.encode("utf-8"))


if __name__ == "__main__":
    main()
