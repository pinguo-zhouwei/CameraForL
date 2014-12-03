Android-DecoCamera
==================
大头贴相机Android版本

代码规范
------------------
1. 阅读代码规范：
http://source.android.com/source/code-style.html  

2. Android Studio提交代码时勾选一下选项：  
![图片](https://github.com/PGClient/Android-DecoCamera/blob/master/doc/res/before_commit.png)

3. 更新说明日志  
如：[马睿]修改大头贴相机照片存储位置

基础组件
------------------

1. 日志  
使用L类(us.pinguo.framework.log.L)，用法同系统Log

2. ButterKnife  
使用ButterKnife替代项目中findView代码，具体使用方法参考：
http://jakewharton.github.io/butterknife/

3. 图片加载  
使用ImageLoaderView异步加载图片，ImageLoaderView封装了UniversalImageLoader开源库
https://github.com/nostra13/Android-Universal-Image-Loader

包划分
------------------
    
    --us                        //顶级域名
    ----pinguo                  //公司域名
    ------decocamera            //项目名称
    --------api                 //服务器接口实现
    --------config              //全局配置项
    --------module              //业务模块
    ----------camera            //相机模块
    ----------edit              //编辑模块
    ----------make              //制作大头贴模块
    ----------order             //订单模块
    ----------welcome           //主页模块
    --------ui                  //通用UI
    --------util                //通用工具
        




