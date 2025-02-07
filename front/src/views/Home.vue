<template>
  <div id="home">
    <div class="logo">
      <img src="@/assets/BILIBILI_LOGO.svg" draggable="false"/>
    </div>

    <div class="input_search">
      <input type="text" placeholder="请输入视频链接..." @input="change" @keyup.enter="download"
             v-model.trim="videoUrl"/>
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

        <!-- 视频下载列表 -->
        <el-collapse-item v-show="downloadUrls.length > 0" title="视频地址" name="视频地址">
          <div v-for="item in downloadUrls" :key="item.cid" class="download-item">
            <p class="video-title">{{ item.title }}</p>
            <p class="video-actions">
              <el-col :span="18">
                <a :href="item.url" target="_blank">预览视频</a>
                <a href v-show="!downVideoStatus[item.cid]" @click.prevent="downVideo(item)">下载视频</a>
                <a href v-show="downVideoStatus[item.cid]" @click.prevent="cancelDownload(item)">取消下载</a>
              </el-col>
              <el-col :span="6">
                <el-select
                    class="quality"
                    v-model="item.qn"
                    placeholder="选择清晰度"
                    @change="getDownloadUrl(item.cid,item.qn)"
                >
                  <el-option
                      v-for="(option, index) in item.Qualities"
                      :label="option.label"
                      :value="option.value"
                  ></el-option>
                </el-select>
              </el-col>
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

      </el-collapse>
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
    };
  },

  methods: {

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

    downVideo(item) {
      // 添加下载状态锁
      if (this.downVideoStatus[item.cid]) return;

      this.$set(this.downVideoStatus, item.cid, true);

      const source = axios.CancelToken.source();
      this.$set(this.sources, item.cid, source);

      axios.get(item.url, {
        responseType: "blob",
        timeout: 3000000, // 添加超时设置
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

        const blob = new Blob([resp.data], {type: 'video/mp4'});
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
              // return this.getDownloadUrl(cid,80)
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
          // console.log(resp.data)
          this.videoName = resp.data.data.title
          this.avid = resp.data.data.aid
          this.bgUrl = resp.data.data.pic
          this.pages = resp.data.data.pages
          this.selectedPages = Array.from({length: this.pages.length}, (_, i) => i)
          resolve()
        })
      })
    },


    // 修改后的获取下载链接方法
    async getDownloadUrl(cid, qn) {
      try {
        const res = await axios.get(`/download/${this.avid}/${cid}?qn=${qn}`);
        console.log(res.data);

        // 提取清晰度选项
        let accept_description = res.data.data.accept_description;
        let accept_quality = res.data.data.accept_quality;
        const Qualities = accept_description.map((q, index) => ({
          value: accept_quality[index],
          label: accept_description[index],
        }));

        // 处理DASH格式的视频和音频
        const videoList = res.data.data.dash.video || [];
        const audioList = res.data.data.dash.audio || [];

        // 示例：选择最高质量的视频和音频流（根据实际需求调整）
        const bestVideo = videoList.find(video => video.id === qn);
        const bestAudio = audioList[0]; // 假设音频只有一种选择

        if (bestVideo && bestAudio) {
          const title = this.pages.find(p => p.cid === cid).part;

          // 更新下载链接（这里我们假设只需要视频链接，实际情况可能需要合成音视频）
          const existing = this.downloadUrls.find(item => item.cid === cid);
          if (existing) {
            existing.url = bestVideo.baseUrl; // 使用视频的基本URL作为下载链接
            existing.audioUrl = bestAudio.baseUrl; // 如果也需要音频链接的话
          } else {
            this.downloadUrls.push({
              cid,
              url: bestVideo.baseUrl,
              audioUrl: bestAudio.baseUrl, // 如果也需要音频链接的话
              title,
              Qualities,
              qn
            });
          }
        } else {
          this.$notify.error({
            title: '获取链接失败',
            message: `CID:${cid} 清晰度${qn}不可用或无音频流`,
            duration: 3000
          });
        }
      } catch (error) {
        this.$notify.error({
          title: '获取链接失败',
          message: `CID:${cid} 请求过程中发生错误`,
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
        await this.getDownloadUrl(cid,80);
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

#home {
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
