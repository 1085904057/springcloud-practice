# Storage Service 设计文档

## 1. 项目概述

Storage Service 是一个融合的对象存储服务，支持客户端将文件上传和下载到任意兼容S3标准的对象存储服务（如AWS S3、IDrive E2、MinIO等），同时对客户端屏蔽不同存储源的真实域名。

## 2. 核心功能

### 2.1 文件上传
- 客户端向Storage-Service请求预签名分片上传URL
- 使用S3 JavaScript SDK执行分片上传
- 支持大文件分片上传
- **支持断点续传功能**

### 2.2 文件下载
- 客户端向Storage-Service请求预签名下载URL
- 直接从对象存储服务下载文件

### 2.3 域名隐藏
- 使用Cloudflare CNAME代理隐藏真实存储源endpoint
- 提供统一的域名访问接口

### 2.4 断点续传
- 页面刷新或网络中断后可恢复上传
- 自动检测已完成的分片，只上传缺失部分
- 本地状态持久化，支持跨会话恢复

## 3. 技术架构

### 3.1 上传流程
```
客户端 -> Storage-Service (请求预签名URL) -> 返回分片上传URL列表
客户端 -> 直接上传到对象存储 (通过Cloudflare代理域名)
客户端 -> Storage-Service (完成分片上传)
```

### 3.2 断点续传流程
```
客户端 -> Storage-Service (查询上传状态) -> 返回已完成分片信息
客户端 -> Storage-Service (请求剩余分片URL) -> 返回未完成分片URL
客户端 -> 继续上传剩余分片 -> 完成上传
```

### 3.3 下载流程
```
客户端 -> Storage-Service (请求预签名下载URL)
客户端 -> 直接从对象存储下载 (通过Cloudflare代理域名)
```

## 4. API 设计

### 4.1 分片上传初始化
```
POST /api/v1/upload/initiate
Request:
{
  "bucket": "my-bucket",
  "key": "path/to/file.jpg",
  "fileSize": 104857600,
  "contentType": "image/jpeg"
}

Response:
{
  "uploadId": "upload-123",
  "partCount": 10,
  "presignedUrls": [
    "https://proxy.example.com/bucket/key?uploadId=123&partNumber=1&...",
    "https://proxy.example.com/bucket/key?uploadId=123&partNumber=2&..."
  ]
}
```

### 4.2 查询上传状态
```
GET /api/v1/upload/status/{uploadId}

Response:
{
  "uploadId": "upload-123",
  "bucket": "my-bucket",
  "key": "path/to/file.jpg",
  "totalParts": 10,
  "completedParts": [
    {"partNumber": 1, "etag": "etag1", "size": 10485760},
    {"partNumber": 2, "etag": "etag2", "size": 10485760}
  ],
  "status": "UPLOADING"
}
```

### 4.3 恢复上传
```
POST /api/v1/upload/resume
Request:
{
  "uploadId": "upload-123",
  "pendingParts": [3, 4, 5, 6, 7, 8, 9, 10]
}

Response:
{
  "uploadId": "upload-123",
  "presignedUrls": [
    "https://proxy.example.com/bucket/key?uploadId=123&partNumber=3&...",
    "https://proxy.example.com/bucket/key?uploadId=123&partNumber=4&..."
  ]
}
```

### 4.4 完成分片上传
```
POST /api/v1/upload/complete
Request:
{
  "uploadId": "upload-123",
  "bucket": "my-bucket",
  "key": "path/to/file.jpg",
  "parts": [
    {"partNumber": 1, "etag": "etag1"},
    {"partNumber": 2, "etag": "etag2"}
  ]
}
```

### 4.5 获取下载URL
```
GET /api/v1/download/presigned?bucket=my-bucket&key=path/to/file.jpg&expires=3600

Response:
{
  "presignedUrl": "https://proxy.example.com/bucket/key?signature=...",
  "expiresIn": 3600
}
```

## 5. 配置管理

### 5.1 存储源配置
```yaml
storage:
  providers:
    aws-s3:
      endpoint: https://s3.amazonaws.com
      proxyEndpoint: https://aws-proxy.example.com
      accessKeyId: ${AWS_ACCESS_KEY}
      secretKey: ${AWS_SECRET_KEY}
      region: us-east-1
    minio:
      endpoint: https://minio.example.com
      proxyEndpoint: https://minio-proxy.example.com
      accessKeyId: ${MINIO_ACCESS_KEY}
      secretKey: ${MINIO_SECRET_KEY}
      region: us-east-1
  upload:
    partSize: 10485760  # 10MB per part
    maxParts: 10000
    presignedUrlExpires: 3600  # 1 hour
```

### 5.2 Cloudflare配置
- 为每个存储源创建CNAME记录
- 开启Proxy代理模式
- 配置缓存规则避免上传请求被缓存

## 6. 核心类设计

### 6.1 配置类
```java
@ConfigurationProperties(prefix = "storage")
public class StorageConfig {
    private Map<String, StorageProvider> providers;
    private UploadConfig upload;
    
    public static class StorageProvider {
        private String endpoint;
        private String proxyEndpoint;
        private String accessKeyId;
        private String secretKey;
        private String region;
    }
    
    public static class UploadConfig {
        private long partSize = 10485760L; // 10MB
        private int maxParts = 10000;
        private int presignedUrlExpires = 3600;
    }
}
```

### 6.2 服务类
```java
@Service
public class UploadService {
    MultipartUploadResponse initiateMultipartUpload(String provider, String bucket, String key, long fileSize);
    UploadStatusResponse getUploadStatus(String uploadId);
    ResumeUploadResponse resumeUpload(String uploadId, List<Integer> pendingParts);
    void completeMultipartUpload(String provider, CompleteMultipartUploadRequest request);
}

@Service
public class DownloadService {
    String generatePresignedDownloadUrl(String provider, String bucket, String key, int expiresIn);
}
```

### 6.3 数据传输对象
```java
public class UploadStatusResponse {
    private String uploadId;
    private String bucket;
    private String key;
    private int totalParts;
    private List<CompletedPart> completedParts;
    private UploadStatus status; // INITIATED, UPLOADING, COMPLETED, ABORTED
}

public class ResumeUploadResponse {
    private String uploadId;
    private List<String> presignedUrls;
    private List<Integer> pendingParts;
}
```

## 7. 客户端SDK示例

### 7.1 JavaScript分片上传（支持断点续传）
```javascript
class UploadManager {
    constructor() {
        this.uploads = this.loadUploadsFromStorage();
    }
    
    // 初始化上传
    async initiateUpload(file, bucket, key) {
        const response = await fetch('/api/v1/upload/initiate', {
            method: 'POST',
            body: JSON.stringify({
                bucket: bucket,
                key: key,
                fileSize: file.size,
                contentType: file.type
            })
        });
        
        const uploadData = await response.json();
        
        // 保存上传状态
        this.saveUploadState(uploadData.uploadId, {
            file: file,
            bucket: bucket,
            key: key,
            totalParts: uploadData.partCount,
            completedParts: [],
            status: 'uploading'
        });
        
        return this.uploadParts(uploadData);
    }
    
    // 恢复上传
    async resumeUpload(uploadId) {
        const savedState = this.uploads[uploadId];
        if (!savedState) return;
        
        // 查询服务端状态
        const statusResponse = await fetch(`/api/v1/upload/status/${uploadId}`);
        const serverStatus = await statusResponse.json();
        
        // 找出未完成的分片
        const completedPartNumbers = serverStatus.completedParts.map(p => p.partNumber);
        const pendingParts = [];
        for (let i = 1; i <= serverStatus.totalParts; i++) {
            if (!completedPartNumbers.includes(i)) {
                pendingParts.push(i);
            }
        }
        
        if (pendingParts.length === 0) {
            // 所有分片已完成，直接完成上传
            return this.completeUpload(uploadId, serverStatus.completedParts);
        }
        
        // 获取剩余分片的预签名URL
        const resumeResponse = await fetch('/api/v1/upload/resume', {
            method: 'POST',
            body: JSON.stringify({
                uploadId: uploadId,
                pendingParts: pendingParts
            })
        });
        
        const resumeData = await resumeResponse.json();
        return this.uploadPendingParts(resumeData, savedState.file, serverStatus.completedParts);
    }
    
    // 上传分片
    async uploadParts(uploadData) {
        const uploadPromises = uploadData.presignedUrls.map(async (url, index) => {
            const partNumber = index + 1;
            const start = index * CHUNK_SIZE;
            const end = Math.min(start + CHUNK_SIZE, file.size);
            const chunk = file.slice(start, end);
            
            const response = await fetch(url, {
                method: 'PUT',
                body: chunk
            });
            
            const etag = response.headers.get('ETag');
            
            // 更新本地状态
            this.updatePartStatus(uploadData.uploadId, partNumber, etag);
            
            return {
                partNumber: partNumber,
                etag: etag
            };
        });
        
        const parts = await Promise.all(uploadPromises);
        return this.completeUpload(uploadData.uploadId, parts);
    }
    
    // 完成上传
    async completeUpload(uploadId, parts) {
        const savedState = this.uploads[uploadId];
        
        await fetch('/api/v1/upload/complete', {
            method: 'POST',
            body: JSON.stringify({
                uploadId: uploadId,
                bucket: savedState.bucket,
                key: savedState.key,
                parts: parts
            })
        });
        
        // 清除本地状态
        this.clearUploadState(uploadId);
    }
    
    // 保存上传状态到localStorage
    saveUploadState(uploadId, state) {
        const uploads = JSON.parse(localStorage.getItem('s3_uploads') || '{}');
        uploads[uploadId] = {
            ...state,
            timestamp: Date.now()
        };
        localStorage.setItem('s3_uploads', JSON.stringify(uploads));
    }
    
    // 页面加载时检查未完成的上传
    async checkPendingUploads() {
        const uploads = JSON.parse(localStorage.getItem('s3_uploads') || '{}');
        const pendingUploads = Object.entries(uploads)
            .filter(([_, upload]) => upload.status === 'uploading')
            .map(([uploadId, _]) => uploadId);
            
        return pendingUploads;
    }
}

// 页面加载时自动检查断点续传
window.addEventListener('load', async () => {
    const uploadManager = new UploadManager();
    const pendingUploads = await uploadManager.checkPendingUploads();
    
    if (pendingUploads.length > 0) {
        showResumeUploadDialog(pendingUploads);
    }
});
```

## 8. Web文件浏览器

### 8.1 S3 Browser功能
- 文件夹浏览和导航
- 文件上传（支持拖拽）
- 文件下载和预览
- 文件删除和重命名
- 断点续传管理

### 8.2 技术实现
```javascript
class S3Browser {
    constructor(config) {
        this.uploadManager = new UploadManager();
        this.currentPath = '';
    }
    
    // 列出文件和文件夹
    async listObjects(bucket, prefix = '') {
        const response = await fetch(`/api/v1/browser/objects?bucket=${bucket}&prefix=${prefix}`);
        return await response.json();
    }
    
    // 文件拖拽上传
    setupDropZone() {
        const dropZone = document.getElementById('drop-zone');
        
        dropZone.addEventListener('drop', async (e) => {
            e.preventDefault();
            const files = Array.from(e.dataTransfer.files);
            
            for (const file of files) {
                const key = this.currentPath + file.name;
                await this.uploadManager.initiateUpload(file, this.currentBucket, key);
            }
        });
    }
}
```

## 9. 部署要求

### 9.1 环境变量
- 各存储源的访问凭证
- Cloudflare配置信息

### 9.2 依赖服务
- Redis (可选，用于缓存预签名URL和上传状态)
- 各对象存储服务

## 10. 安全考虑

- 预签名URL有效期控制
- 访问权限验证
- 文件类型和大小限制
- 防止恶意上传
- 上传状态信息安全存储

## 11. 监控指标

- 上传成功率
- 断点续传使用率
- 下载响应时间
- 存储源可用性
- 带宽使用情况
- 分片上传完成时间分布