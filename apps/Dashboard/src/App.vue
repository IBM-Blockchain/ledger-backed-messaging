<template>
  <v-app>
    <v-app-bar app color="primary" dark>
      <div class="d-flex align-center"></div>

           <v-toolbar-title>Ledger-backed Messaging</v-toolbar-title>

      <v-spacer></v-spacer>

      <router-link to="/trades">
        <span class="white--text ml-5">Submitted Trades</span>
      </router-link>
      
      <p></p>
      <router-link to="/submit">
        <span class="white--text ml-5">New Trade</span>
      </router-link>


    </v-app-bar>

    <v-main>
      <router-view></router-view>
    </v-main>
  </v-app>
</template>

<script>
export default {
  name: "App",

  components: {},

  data: () => ({
    //
  }),
};
</script>
