import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import router from './router'

Vue.config.productionTip = false
Vue.use(ElementUI)

import { Notification } from 'element-ui';
Vue.prototype.$notify = Notification;

import VueCookies from 'vue-cookies';
Vue.use(VueCookies);


new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
