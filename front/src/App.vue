<template>
  <div id="app">
    <div class="logo">
      <img src="./assets/BILIBILI_LOGO.svg" draggable="false" />
    </div>
    <div class="input_search">
      <input type="text" placeholder="请输入视频链接..." @input="change" @keyup.enter="download" v-model.trim="videoUrl" />
      <button @click="download">获取</button>
    </div>
    <div class="use-info">
      <el-collapse :accordion="true" v-model="activeNames">
        <!-- 分集选择 -->
        <el-collapse-item v-show="pages.length > 1" title="选择分集" name="分集">
          <el-checkbox-group v-model="selectedPages">
            <el-checkbox
                v-for="(page, index) in pages"
                :key="index"
                :label="index"
                class="page-checkbox"
            >
              {{ page.part }} (P{{ index + 1 }})
            </el-checkbox>
          </el-checkbox-group>
        </el-collapse-item>
<!--        &lt;!&ndash; 清晰度选择 &ndash;&gt;-->
<!--        <el-collapse-item v-show="pages.length > 0" title="选择清晰度" name="清晰度">-->
<!--          <el-select v-model="selectedQuality" placeholder="请选择清晰度" class="quality-select">-->
<!--            <el-option-->
<!--                v-for="option in qualityOptions"-->
<!--                :key="option.value"-->
<!--                :label="option.label"-->
<!--                :value="option.value">-->
<!--            </el-option>-->
<!--          </el-select>-->
<!--        </el-collapse-item>-->

        <!-- 修改后的清晰度选择 -->
        <el-collapse-item v-show="selectedPages.length > 0" title="视频清晰度" name="quality">
          <div class="quality-selector">
            <el-select
                v-model="selectedQuality"
                placeholder="选择清晰度"
                :disabled="!selectedPages.length"
            >
              <el-option
                  v-for="(option, cid) in currentVideoQualities"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                  :disabled="option.disabled"
              >
        <span :class="{'vip-quality': option.value > 80}">
          {{ option.label }}
          <i v-if="option.disabled" class="el-icon-warning">需大会员</i>
        </span>
              </el-option>
            </el-select>

            <div class="quality-tip">
              <i class="el-icon-info"></i>
              <span v-if="selectedQuality > 80">当前选择为大会员专属清晰度</span>
              <span v-else>默认选择最佳可用清晰度</span>
            </div>
          </div>
        </el-collapse-item>

        <!-- 视频下载列表 -->
        <el-collapse-item v-show="downloadUrls.length > 0" title="视频地址" name="视频地址">
          <div v-for="item in downloadUrls" :key="item.cid" class="download-item">
            <p class="video-title">{{ item.title }}</p>
            <p class="video-url">{{ item.url }}</p>
            <p class="video-actions">
              <a :href="item.url" target="_blank">预览视频</a>
              <a href v-show="!downVideoStatus[item.cid]" @click.prevent="downVideo(item)">下载视频</a>
              <a href v-show="downVideoStatus[item.cid]" @click.prevent="cancelDownload(item)">取消下载</a>
              <el-progress
                  v-show="downVideoStatus[item.cid]"
                  :percentage="progress[item.cid]"
                  :format="format"
              ></el-progress>
            </p>
          </div>
        </el-collapse-item>

        <!-- 封面地址 -->
        <el-collapse-item v-show="bgUrl != ''" title="封面地址" name="2">
          <p>{{ imgUrl }}</p>
          <p>
            <a rel="noreferrer" :href="imgUrl" target="_blank">预览图片</a>
          </p>
        </el-collapse-item>

        <!-- 使用说明 -->
        <el-collapse-item title="使用说明" name="3">
          <p>1. 支持分P视频下载，可多选分集</p>
          <p>2. 手机分集视频，分享在浏览器打开，再复制地址栏链接下载</p>
          <p>3. 每个分集会独立显示下载进度</p>
          <p>4. 点击取消可停止当前下载</p>
          <p>5. 图片可以长按进行保存</p>
          <p>6. 电脑可用Ctrl+B快速清空输入</p>
        </el-collapse-item>
      </el-collapse>
    </div>
    <div class="tip">
      仅供学习使用
    </div>
  </div>
</template>

<script>
import axios from "axios"
axios.defaults.baseURL = "http://localhost:8989"

export default {
  mounted() {
    document.body.addEventListener("keydown", e => {
      if (e.ctrlKey && e.keyCode == 66) {
        this.videoUrl = "";
      }
    });

    setTimeout(() => {
      this.$notify({
        title: "欢迎使用",
        message: "欢迎使用本工具，愿你有个好心情✨",
        type: "success",
        duration: 2500
      });
    }, 200);

    document.querySelector("input").focus();
  },

  data() {
    return {
      activeNames: "",
      videoUrl: "",
      bgUrl: "",
      videoName: "",
      bv: "",
      avid: "",
      p: "",
      pages: [],          // 所有分集信息
      selectedPages: [],  // 选中的分集索引
      downloadUrls: [],   // 分集下载链接
      downVideoStatus: {}, // 下载状态 {cid: boolean}
      progress: {},        // 下载进度 {cid: percentage}
      sources: {},          // 取消令牌 {cid: source}
      // 修改后的清晰度相关数据
      selectedQuality: null,
      qualityOptions: [],
      currentVideoQualities: {}, // {cid: [qualityOptions]}
    };
  },

  methods: {

    // 新增获取清晰度方法
    async getAvailableQualities(cid) {
      try {
        const res = await axios.get(`/qualities/${this.avid}/${cid}`);
        const qualities = res.data.data.accept_quality;
        const descriptions = res.data.data.accept_description;

        return qualities.map((q, index) => ({
          value: q,
          label: `${descriptions[index]} (${this.formatQuality(q)})`,
          disabled: !res.data.data.accept_quality.includes(q)
        }));
      } catch (error) {
        console.error('获取清晰度失败:', error);
        return [];
      }
    },

    // 清晰度代码转换
    formatQuality(qn) {
      const qualityMap = {
        127: '8K',
        126: 'Dolby',
        120: '4K',
        116: '1080P60',
        112: '1080P+',
        80: '1080P',
        64: '720P',
        32: '480P',
        16: '360P'
      };
      return qualityMap[qn] || qn.toString();
    },


    // 取消单个下载
    cancelDownload(item) {
      const source = this.sources[item.cid]
      if (source) {
        source.cancel('用户取消下载')
        this.$set(this.downVideoStatus, item.cid, false)
        this.$set(this.progress, item.cid, 0)
        this.$notify({
          title: "已取消",
          message: `已取消下载：${item.title}`,
          type: "warning",
          duration: 2000
        })
      }
    },

    // 下载单个视频
    // downVideo(item) {
    //   this.$set(this.downVideoStatus, item.cid, true)
    //   this.$set(this.progress, item.cid, 0)
    //
    //   const source = axios.CancelToken.source()
    //   this.$set(this.sources, item.cid, source)
    //
    //   this.$notify({
    //     title: "开始下载",
    //     message: `正在下载：${item.title}`,
    //     type: "info",
    //     duration: 2000
    //   })
    //
    //   axios.get(item.url, {
    //     responseType: "blob",
    //     cancelToken: source.token,
    //     onDownloadProgress: evt => {
    //       const percent = parseInt((evt.loaded / evt.total) * 100)
    //       this.$set(this.progress, item.cid, percent)
    //     }
    //   }).then(resp => {
    //     const blobUrl = window.URL.createObjectURL(resp.data)
    //     const a = document.createElement("a")
    //     a.download = `${item.title}.mp4`
    //     a.href = blobUrl
    //     document.body.appendChild(a)
    //     a.click()
    //     a.remove()
    //     window.URL.revokeObjectURL(blobUrl)
    //
    //     this.$set(this.downVideoStatus, item.cid, false)
    //     this.$notify({
    //       title: "下载完成",
    //       message: `成功下载：${item.title}`,
    //       type: "success",
    //       duration: 2000
    //     })
    //   }).catch(error => {
    //     if (!axios.isCancel(error)) {
    //       this.$notify.error({
    //         title: "下载失败",
    //         message: `${item.title} 下载失败：${error.message}`,
    //         duration: 2000
    //       })
    //     }
    //   })
    // },
    downVideo(item) {
      // 添加下载状态锁
      if (this.downVideoStatus[item.cid]) return;

      this.$set(this.downVideoStatus, item.cid, true);

      const source = axios.CancelToken.source();
      this.$set(this.sources, item.cid, source);

      axios.get(item.url, {
        responseType: "blob",
        timeout: 30000, // 添加超时设置
        headers: {
          "Referer": "https://www.bilibili.com" // 添加必要请求头
        },
        cancelToken: source.token,
        onDownloadProgress: evt => {
          const percent = parseInt((evt.loaded / evt.total) * 100);
          this.$set(this.progress, item.cid, percent);
        }
      }).then(resp => {
        if (resp.status !== 200 || !resp.data.type.includes('video')) {
          throw new Error('无效的视频文件');
        }

        const blob = new Blob([resp.data], { type: 'video/mp4' });
        const downloadUrl = URL.createObjectURL(blob);

        const a = document.createElement("a");
        a.style.display = "none";
        a.href = downloadUrl;
        a.download = `${item.title}.mp4`;

        document.body.appendChild(a);
        a.click();

        // 清理资源
        URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);

      }).catch(error => {
        if (!axios.isCancel(error)) {
          this.$notify.error({
            title: "下载失败",
            message: `失败原因：${error.message || '未知错误'}`,
            duration: 3000
          });
        }
      }).finally(() => {
        this.$set(this.downVideoStatus, item.cid, false);
      });
    },

    // 进度显示格式
    format(percentage) {
      return percentage === 100 ? "完成" : `${percentage}%`
    },

    // 输入变化时重置
    change() {
      this.downloadUrls = []
      this.pages = []
      this.selectedPages = []
      this.bgUrl = ""
      Object.values(this.sources).forEach(source => source.cancel())
      this.downVideoStatus = {}
      this.progress = {}
    },

    // 主下载流程
    download() {
      this.change()
      this.parseUrl().then(() => {
        if (this.bv) {
          this.getAvidCidByBv().then(() => {
            const requests = this.selectedPages.map(index => {
              const cid = this.pages[index].cid
              return this.getDownloadUrl(cid)
            })
            Promise.all(requests)
          })
        }
      })
    },

    // 解析URL
    parseUrl() {
      return new Promise((resolve) => {
        if (this.videoUrl.includes("?p=")) {
          this.p = this.videoUrl.split("?p=")[1].split("&")[0]
          this.bv = this.videoUrl.split("/video/")[1].split("?p=")[0]
          resolve()
        } else if (this.videoUrl.includes("https://b23.tv")) {
          axios.get("/move?url=" + this.videoUrl).then(resp => {
            this.videoUrl = resp.data.split(`<meta data-vue-meta="true" itemprop="url" content="`)[1].split(`/">`)[0]
            this.bv = this.videoUrl.split("/video/")[1].split("/?")[0]
            resolve()
          })
        } else {
          this.bv = this.videoUrl.split("/video/")[1].split("/?")[0]
          resolve()
        }
      })
    },

    // 获取视频基本信息
    getAvidCidByBv() {
      return new Promise((resolve) => {
        axios.get(`/av/${this.bv}`).then(resp => {
          this.videoName = resp.data.data.title
          this.avid = resp.data.data.aid
          this.bgUrl = resp.data.data.pic
          this.pages = resp.data.data.pages
          this.selectedPages = Array.from({length: this.pages.length}, (_, i) => i)
          resolve()
        })
      })
    },

    // 获取单个分P下载地址
    // getDownloadUrl(cid) {
    //   return new Promise((resolve) => {
    //     axios.get(`/download/${this.avid}/${cid}?qn=${this.selectedQuality}`)
    //         .then(resp => {
    //           const url = resp.data.data.durl[0].url;
    //           const title = this.pages.find(p => p.cid === cid).part;
    //           this.downloadUrls.push({ cid, url, title });
    //           resolve();
    //         });
    //   });
    // },

    // 修改后的获取下载链接方法
    async getDownloadUrl(cid) {
      if (!this.selectedQuality) {
        this.$message.warning('请先选择清晰度');
        return;
      }

      try {
        const res = await axios.get(`/download/${this.avid}/${cid}?qn=${this.selectedQuality}`);
        const url = res.data.data.durl[0].url;
        const title = this.pages.find(p => p.cid === cid).part;

        // 更新下载链接
        const existing = this.downloadUrls.find(item => item.cid === cid);
        if (existing) {
          existing.url = url;
        } else {
          this.downloadUrls.push({ cid, url, title });
        }
      } catch (error) {
        this.$notify.error({
          title: '获取链接失败',
          message: `CID:${cid} 清晰度${this.selectedQuality}不可用`,
          duration: 3000
        });
      }
    },

    // 修改后的分集选择处理
    async handlePageSelection() {
      this.downloadUrls = [];

      // 获取每个分集的可用清晰度
      for (const index of this.selectedPages) {
        const cid = this.pages[index].cid;
        if (!this.currentVideoQualities[cid]) {
          this.currentVideoQualities[cid] = await this.getAvailableQualities(cid);
        }

        // 默认选择第一个可用清晰度
        if (!this.selectedQuality) {
          const firstAvailable = this.currentVideoQualities[cid].find(q => !q.disabled);
          if (firstAvailable) {
            this.selectedQuality = firstAvailable.value;
          }
        }

        await this.getDownloadUrl(cid);
      }
    }
  },

  computed: {
    imgUrl() {
      return this.bgUrl ? "https" + this.bgUrl.substring(4) : ""
    }
  },
  watch: {
    // 监听清晰度变化
    selectedQuality(newVal) {
      if (newVal) {
        this.handlePageSelection();
      }
    },

    // 监听分集选择变化
    selectedPages() {
      this.handlePageSelection();
    }
  }
};
</script>

<style lang="less">
.quality-select {
  width: 100%;
  margin-bottom: 10px;
}

.quality-select .el-select {
  width: 100%;
}
.page-checkbox {
  display: block;
  margin: 8px 0;
  .el-checkbox__label {
    font-size: 14px;
    color: #666;
  }
}

.download-item {
  margin: 15px 0;
  padding: 12px;
  border-radius: 6px;
  background: #f8f8f8;

  .video-title {
    font-weight: 500;
    color: #333;
    margin-bottom: 6px;
  }

  .video-url {
    font-size: 12px;
    color: #666;
    word-break: break-all;
    margin-bottom: 10px;
  }

  .video-actions {
    a {
      margin-right: 15px;
      padding: 4px 12px;
      border-radius: 15px;
      background: #e8f4ff;
      color: #1296db;
      transition: all 0.2s;

      &:hover {
        background: #1296db;
        color: white;
      }
    }
  }
}

.el-progress {
  margin-top: 10px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.content {
  padding: 16px 16px 160px;
}

#app {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: column;
  min-height: 100vh;
  .logo {
    width: 35%;
    display: flex;
    justify-content: center;
    align-items: center;
    img {
      width: 30%;
      margin-top: 200px;
      user-select: none;
    }
  }
  .input_search {
    width: 45%;
    margin-top: 50px;
    display: flex;
    align-items: center;
    justify-content: center;
    input {
      width: 85%;
      height: 45px;
      padding-right: 20px;
      border: 1px solid #888;
      border-right: none;
      border-radius: 7px 0 0 7px;
      font-size: 16px;
      color: #363636;
      outline: none;
      text-indent: 0.7em;
      &::placeholder {
        color: #979797;
        font-size: 14px;
      }
    }
    button {
      width: 15%;
      height: 45px;
      border: none;
      background-color: #1296db;
      border-radius: 0 7px 7px 0;
      font-size: 14px;
      cursor: pointer;
      position: relative;
      letter-spacing: 0.1em;
      color: #fff;
      padding: 2px 4px;
      outline: none;
      user-select: none;
    }
  }
  .use-info {
    width: 44.5%;
    user-select: none;
  }
  .tip {
    position: fixed;
    bottom: 0px;
    left: 50%;
    width: 100%;
    height: 30px;
    background-color: #fefefeed;
    text-align: center;
    transform: translateX(-50%);
    font-size: 12px;
    color: #949494;
    display: flex;
    align-items: center;
    justify-content: center;
    a {
      text-decoration: none;
      color: #1296db;
    }
  }
  .van-cell::after {
    border: none;
  }
  @media screen and (max-width: 600px) {
    .logo {
      width: 55%;
      img {
        width: 50%;
      }
    }
    .input_search {
      width: 90%;
      input {
        width: 85%;
        height: 40px;
      }
      button {
        width: 15%;
        height: 40px;
      }
    }
    .use-info {
      width: 89%;
    }
  }
  .el-collapse-item__header {
    border: none;
  }
  .el-collapse-item__content {
    padding-bottom: 7px;
    p {
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      color: #888;
      padding: 0 0 10px 0;
      a {
        display: inline-block;
        background-color: #eee;
        color: #1296db;
        font-weight: 700;
        padding: 0px 10px;
        border-radius: 100px;
        margin-right: 20px;
        height: 21px;
        line-height: 21px;
        font-size: 12px;
        text-decoration: none;
        transition: background-color 0.3s, color 0.1s;
        &:hover {
          background-color: #1296db;
          color: #fcfcfc;
        }
      }
    }
  }
  .tablist {
    margin-bottom: 100px;
  }
  .el-progress-bar__inner {
    background-color: #1296db;
  }
  .el-progress-bar {
    padding-top: 12px;
  }
  .el-progress__text {
    margin-left: 12px;
  }
}
</style>
