---
title: explain工具
date: 2022-03-22
desc:
keywords: explain
categories: [database]
---
# 介绍

使用EXPLAIN关键字可以模拟优化器执行SQL语句，分析你的查询语句或是结构的性能瓶颈在select语句之前增加 xplain 关键字，
MySQL 会在查询上设置一个标记，执行查询会返 回执行计划的信息， 而不是执行这条SQL 注意：如果 from 中包含子查询，
仍会执行该子查询，将结果放入临时表中

# explain中的列
