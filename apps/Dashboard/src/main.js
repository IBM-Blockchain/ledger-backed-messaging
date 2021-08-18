import Vue from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify';
import axios from 'axios'
import VueAxios from 'vue-axios'
import VueRouter from 'vue-router'
import TradeView from './views/TradeView'
import LedgerableEventView from './views/LedgerableEventView'
import SubmitTradeView from './views/SubmitTradeView'

import 'roboto-fontface/css/roboto/roboto-fontface.css'
import '@mdi/font/css/materialdesignicons.css'

Vue.config.productionTip = false
Vue.use(VueAxios, axios)
Vue.use(VueRouter)

const routes = [
  { path: '/trades', component: TradeView },
  { path: '/submit', component: SubmitTradeView },
  { path: '/ledgerable/:id', component: LedgerableEventView }
]

// keep it simple for now.
const router = new VueRouter({
  routes // short for `routes: routes`
})


new Vue({
  vuetify,
  router,
  render: h => h(App)
}).$mount('#app')
