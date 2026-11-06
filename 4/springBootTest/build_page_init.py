# -*- coding: utf-8 -*-
"""重新拼接 star_page_init.sql（独立交付版建库脚本）
内容 = CREATE DATABASE + schema.sql 全部建表 DDL + data.sql 全部种子 + 演示账号 admin
用法: python build_page_init.py  (在 springBootTest 目录下运行)
"""
from pathlib import Path

ROOT = Path(r"D:/workSpace/java/4")
RES = ROOT / "springBootTest" / "src" / "main" / "resources"
OUT = ROOT / "star_page_init.sql"

ADMIN_HASH = "$2a$10$uf50FiDCaTIukl0htoV5o.7PtPU19kevLAZg6OY7jTG/Fi/nGBAoC"  # 123456

header = """-- ============================================================================
-- 涂山人物志 · 独立交付版建库脚本（自动生成，勿手改）
-- 生成方式: springBootTest/build_page_init.py（拼接 schema.sql + data.sql）
-- 内容: CREATE DATABASE swallowed_star + 全部 7 张表 DDL + 全部种子数据
--       （4 分类 / 18 档案 / 10 委托）+ 演示账号 admin（BCrypt，口令 123456）
-- 用法: mysql -uroot -p123456 --default-character-set=utf8mb4 < star_page_init.sql
-- 全幂等: CREATE TABLE IF NOT EXISTS + INSERT IGNORE，重复执行不产生重复数据。
-- 注意: 后端 schema.sql / data.sql 修改后，需重新运行生成脚本同步本文件。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS swallowed_star DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE swallowed_star;

"""

footer = f"""
-- ============================================================================
-- 演示账号 admin / 123456（BCrypt）
-- ============================================================================
INSERT IGNORE INTO star_user (id, username, password, nickname, status) VALUES
(1, 'admin', '{ADMIN_HASH}', '涂山红娘', 1);
"""

schema = (RES / "schema.sql").read_text(encoding="utf-8")
data = (RES / "data.sql").read_text(encoding="utf-8")

# 去掉两个源文件自身的头部大注释块中与后端启动相关的描述，保留原文即可（无副作用）
content = header + schema.strip() + "\n\n" + data.strip() + "\n" + footer
OUT.write_text(content, encoding="utf-8")
print("written:", OUT, len(content), "chars")
