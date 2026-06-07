#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : sqlutil.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 数据库操作工具类

import mysql.connector
import os
import re
from config.settings import DATABASE_CONFIG
from config import log_config

log = log_config.get_logger(__file__)


class SqlUtil:
    """
    MySQL数据库操作工具类
    """
    
    # 初始化SQL脚本路径
    INIT_SQL_PATH = os.path.join(
        os.path.dirname(__file__), 
        '..', '..', 
        'V1__init.sql'
    )
    
    @staticmethod
    def get_connection():
        """
        获取数据库连接
        :return: 数据库连接对象
        """
        try:
            conn = mysql.connector.connect(
                host=DATABASE_CONFIG['host'],
                port=DATABASE_CONFIG['port'],
                database=DATABASE_CONFIG['database'],
                user=DATABASE_CONFIG['username'],
                password=DATABASE_CONFIG['password'],
                charset='utf8mb4'
            )
            return conn
        except Exception as e:
            log.error(f"数据库连接失败: {e}")
            raise
    
    @staticmethod
    def execute_sql(sql, params=None):
        """
        执行SQL语句
        :param sql: SQL语句
        :param params: 参数列表
        :return: 执行结果
        """
        conn = None
        try:
            conn = SqlUtil.get_connection()
            cursor = conn.cursor(dictionary=True)
            cursor.execute(sql, params)
            conn.commit()
            return cursor.lastrowid
        except Exception as e:
            log.error(f"SQL执行失败: {sql}, params: {params}, error: {e}")
            if conn:
                conn.rollback()
            raise
        finally:
            if conn:
                conn.close()
    
    @staticmethod
    def query_sql(sql, params=None):
        """
        查询SQL语句
        :param sql: SQL语句
        :param params: 参数列表
        :return: 查询结果列表
        """
        conn = None
        try:
            conn = SqlUtil.get_connection()
            cursor = conn.cursor(dictionary=True)
            cursor.execute(sql, params)
            return cursor.fetchall()
        except Exception as e:
            log.error(f"SQL查询失败: {sql}, params: {params}, error: {e}")
            raise
        finally:
            if conn:
                conn.close()
    
    @staticmethod
    def query_one(sql, params=None):
        """
        查询单条记录
        :param sql: SQL语句
        :param params: 参数列表
        :return: 单条记录
        """
        result = SqlUtil.query_sql(sql, params)
        return result[0] if result else None

    @staticmethod
    def reset_database_from_init_sql():
        """
        从初始化SQL脚本重置数据库
        执行V1__init.sql中的INSERT语句恢复初始数据
        如果数据库连接失败，不会抛出异常，只是记录警告
        """
        sql_path = os.path.abspath(SqlUtil.INIT_SQL_PATH)
        if not os.path.exists(sql_path):
            log.warning(f"初始化SQL脚本不存在: {sql_path}")
            return
        
        log.info(f"开始从初始化SQL脚本重置数据库: {sql_path}")
        
        conn = None
        try:
            # 先尝试获取连接
            conn = SqlUtil.get_connection()
            
            # 读取SQL文件
            with open(sql_path, 'r', encoding='utf-8') as f:
                sql_content = f.read()
            
            # 提取INSERT语句
            insert_statements = re.findall(
                r'INSERT INTO[^;]+;',
                sql_content, 
                re.IGNORECASE | re.DOTALL
            )
            
            if not insert_statements:
                log.warning("未找到INSERT语句")
                return
            
            cursor = conn.cursor()
            
            # 先清空相关表（注意清空顺序，考虑外键依赖关系）
            tables_to_clear = [
                # 先清空有外键依赖的表
                'task_execution_result',
                'task_metric_item',
                'metric_stat_item',
                'metric_collection_item',
                'field_metadata',
                'ai_recommendation',
                # 再清空被依赖的表
                'metric_config',
                'metric_collection',
                'metric_category',
                'data_source',
                'task',
                'asset'
            ]
            
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
            for table in tables_to_clear:
                try:
                    cursor.execute(f"TRUNCATE TABLE {table}")
                    #log.debug(f"清空表 {table} 成功")
                except Exception as e:
                    log.debug(f"清空表 {table} 失败（可能不存在）: {e}")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            
            # 执行INSERT语句，按正确的顺序（避免外键依赖问题）
            success_count = 0
            
            # 先执行依赖较少的表的INSERT，再执行有外键的表
            # 按表名分类执行
            table_order = ['metric_category', 'data_source', 'metric_config', 
                         'field_metadata', 'metric_stat_item', 'metric_collection',
                         'metric_collection_item', 'task', 'asset']
            
            # 按顺序执行各表的INSERT
            for table_name in table_order:
                for insert_sql in insert_statements:
                    if f'INSERT INTO `{table_name}`' in insert_sql or f'INSERT INTO {table_name}' in insert_sql:
                        try:
                            # 使用 INSERT IGNORE 避免重复键错误
                            ignore_sql = insert_sql.replace('INSERT INTO', 'INSERT IGNORE INTO')
                            cursor.execute(ignore_sql)
                            success_count += 1
                            # log.debug(f"执行表 {table_name} 的INSERT成功")
                        except Exception as e:
                            log.debug(f"执行表 {table_name} 的INSERT失败: {str(e)[:200]}")
            
            conn.commit()
            log.info(f"数据库重置成功，执行了 {success_count} 条INSERT语句")
            
        except Exception as e:
            log.warning(f"数据库重置失败（可能数据库服务未启动或配置不正确）: {e}")
            if conn:
                try:
                    conn.rollback()
                except:
                    pass
        finally:
            if conn:
                try:
                    conn.close()
                except:
                    pass
