<template>
  <v-container>
    <h1 style="text-align: center">Submitted Trades</h1>        
    <v-btn class="mx-2" fab icon small @click="readDataFromAPI()">
          <v-icon dark>mdi-refresh</v-icon>
        </v-btn>
    <v-data-table
      :headers="headers"
      :items="trades"
      :loading="loading"
      class="elevation-1"
    >
      <template v-slot:[`item.controls`]="props">
        <v-btn class="mx-2" icon fab small @click="onButtonClick(props.item)">
          <v-icon dark>mdi-book</v-icon>
        </v-btn>
      </template>
    </v-data-table>
  </v-container>
</template>

<script>
import axios from "axios";

const APIHOST = "http://localhost/api/tradeengine";

export default {
  components: {},
  name: "TradeView",

  data() {
    return {
      trades: [],
      loading: true,
      options: {},
      headers: [
        { text: "TradeId", value: "tradeId" },
        { text: "Description", value: "offerMsg.description" },
        { text: "Offer Price", value: "offerMsg.price" },
        { text: "Offer Qty", value: "offerMsg.qty" },
                { text: "Actual Price", value: "responseMsg.price" },
        { text: "Actual Qty", value: "responseMsg.qty" },
        { text: "", value: "controls", sortable: false },
      ],
    };
  },
  watch: {
    options: {
      handler() {
        this.readDataFromAPI();
      },
    },
    deep: true,
  },
  methods: {
    onButtonClick(e) {
      this.$router.push({ path: `/ledgerable/${e.tradeId}` });
    },
    readDataFromAPI() {
      this.loading = true;
      axios.get(`${APIHOST}/trader/bob/trade`).then((response) => {
        this.loading = false;
        this.trades = response.data;
        console.log(JSON.stringify(response.data))
      });
    },
  },
  mounted() {
    this.readDataFromAPI();
  },
};
</script>
