<template>
  <div id="app">
    <el-container>
      <el-aside>
        <el-menu
            :default-active="this.$route.path" router
            class="nav-side">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/downloader">下载</el-menu-item>
          <el-menu-item index="/login">登录</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view/>
      </el-main>
    </el-container>
  </div>
</template>
<script>
</script>
<style>
.el-aside {
  width: 100px!important;
}
.nav-side {
  height: 100vh;
  width: 100px;
}
.el-menu-item {
  text-align: center;
}
</style>