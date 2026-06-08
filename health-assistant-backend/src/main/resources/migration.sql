-- 数据库迁移脚本: 为已有 sys_user 表添加新字段
USE smart_health_db;

ALTER TABLE `sys_user`
  ADD COLUMN IF NOT EXISTS `avatar_url` varchar(512) DEFAULT NULL COMMENT '头像链接',
  ADD COLUMN IF NOT EXISTS `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  ADD COLUMN IF NOT EXISTS `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  ADD COLUMN IF NOT EXISTS `bio` varchar(255) DEFAULT NULL COMMENT '个人简介',
  ADD COLUMN IF NOT EXISTS `role` varchar(32) NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN';
