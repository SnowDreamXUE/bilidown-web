import Vue from 'vue'
import Vuex from 'vuex'
Vue.use(Vuex)

const store = new Vuex.Store({
    state: {
        videoUrl: '',
        bgUrl: '',
        avid: '',
        p: '',
        pages: [],          // 所有分集信息
        selectedPages: [],  // 选中的分集索引
    },

    mutations: {
        setVideoUrl(state, videoUrl) {
            state.videoUrl = videoUrl
        },
        setBgUrl(state, bgUrl) {
            state.bgUrl = bgUrl
        },
        setAvid(state, avid) {
            state.avid = avid
        },
        setP(state, p) {
            state.p = p
        },
        setPages(state, pages) {
            state.pages = pages
        },
        setSelectedPages(state, selectedPages) {
            state.selectedPages = selectedPages
        },
        setDownloadUrls(state, downloadUrls) {
            state.downloadUrls = downloadUrls
        },
    },

    actions: {
        setVideoUrl(context, videoUrl) {
            context.commit('setVideoUrl', videoUrl)
        },
        setBgUrl(context, bgUrl) {
            context.commit('setBgUrl', bgUrl)
        },
        setAvid(context, avid) {
            context.commit('setAvid', avid)
        },
        setP(context, p) {
            context.commit('setP', p)
        },
        setPages(context, pages) {
            context.commit('setPages', pages)
        },
        setSelectedPages(context, selectedPages) {
            context.commit('setSelectedPages', selectedPages)
        },
        setDownloadUrls(context, downloadUrls) {
            context.commit('setDownloadUrls', downloadUrls)
        },
        saveData(context, data) {
            context.commit('setBgUrl', data.bgUrl)
            context.commit('setAvid', data.avid)
            context.commit('setP', data.p)
            context.commit('setPages', data.pages)
            context.commit('setSelectedPages', data.selectedPages)
            context.commit('setDownloadUrls', data.downloadUrls)
        },

        clearData(context) {
            context.commit('setAvid', '')
            context.commit('setP', '')
            context.commit('setPages', [])
            context.commit('setSelectedPages', [])
            context.commit('setDownloadUrls', [])
        }
    },

})

export default store