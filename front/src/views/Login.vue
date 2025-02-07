<template>
  <div class="login">
      <el-button type="primary" @click="login_url()">生成登录二维码</el-button>
      <canvas id="qrcode"></canvas>
  </div>
</template>

<script>
import QRCode from "qrcode";
import axios from "axios";
axios.defaults.baseURL = "http://localhost:8989"

export default {
  name: 'Login',
  data() {
    return {
      login_image_url: "",// 登录二维码
      qrcode_key: "",// 登录二维码key
    }
  },
  methods: {
    // 生成登录二维码
    login_url() {
      axios.get("/login_url").then(res => {
        if (res.status === 200) {
          this.login_image_url = res.data.data.url
          this.qrcode_key = res.data.data.qrcode_key
          QRCode.toCanvas(document.getElementById("qrcode"), this.login_image_url, {width: 200}, (error) => {
            if (error) {
              console.error(error)
            }
          })
          this.pollLoginStatus()
        }
      })
    },

    // 轮询登录状态
    pollLoginStatus() {
      const POLL_INTERVAL = 3000; // 轮询间隔时间，单位为毫秒
      const MAX_POLL_TIME = 180000; // 最大轮询时间，单位为毫秒（180秒）
      const qrcode_key = this.qrcode_key;

      function login_check() {
        axios.get("/login_check", {
          params: {
            qrcode_key: qrcode_key
          }
        }).then(response => {
          const data = response.data.data;

          if (data.code === 0) {
            // 登录成功
            console.log('登录成功:', data);
            clearInterval(pollingIntervalId); // 停止轮询
          } else if (data.code === 86038) {
            // 二维码已失效
            console.error('二维码已失效');
            clearInterval(pollingIntervalId); // 停止轮询
          } else if (data.code === 86090 || data.code === 86101) {
            // 继续轮询
            console.log('等待二维码扫描...');
          }
        })
            .catch(error => {
              console.error('获取登录状态失败:', error);
              clearInterval(pollingIntervalId); // 遇到错误时停止轮询
            });
      }

      const pollingIntervalId = setInterval(login_check, POLL_INTERVAL);

      // 设置最大轮询时间
      setTimeout(() => {
        clearInterval(pollingIntervalId);
        console.error('轮询超时，二维码可能已过期');
      }, MAX_POLL_TIME);
    },

  }
}
</script>

<style scoped>
.login {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

</style>