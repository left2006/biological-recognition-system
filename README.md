# Marine Species Recognition Platform 前后端启动说明

本项目包含后端（Spring Boot，Java 21）和前端（Vue 3 + Vite）两部分。

---

## 一、后端启动

1. **环境要求**：
   - JDK 17 及以上
   - Maven 3.6+
   - MySQL 数据库（请根据 `mrsp/src/main/resources/application.yml` 配置数据库连接）

2. **启动方式**：
   - 进入后端目录（`mrsp`）
   - 使用命令行运行：
     ```bash
     mvn spring-boot:run
     ```
   - 或者直接运行主类：
     `com.gec.marine.MarineRecognitionApplication`

---

## 二、前端启动

1. **环境要求**：
   - Node.js 16 及以上
   - npm 8 及以上

2. **安装依赖**：
   - 进入前端目录（`mrsp_web`）
   - 执行：
     ```bash
     npm install
     ```

3. **启动开发服务器**：
   - 执行：
     ```bash
     npm run dev
     ```
   - 启动成功后，终端会显示本地访问地址（如：http://localhost:5173/）。

---

## 三、访问系统

1. **确保后端服务已启动并正常连接数据库。**
2. **前端开发服务器启动后，浏览器访问终端输出的网址即可进入系统界面。**

---

## 其他说明
- 如需打包前端用于生产环境，请执行 `npm run build`，生成的静态文件位于 `dist/` 目录。
- 如遇依赖或环境问题，请根据报错信息检查 Node、npm、JDK、Maven 版本。
