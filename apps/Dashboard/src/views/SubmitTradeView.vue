<template>
  <v-container>
    <h1 style="text-align: center">New Trade Bid Submission</h1>
    <v-container>
      <p>
        Enter details of what you a bidding to supply and at what price point
      </p>
    </v-container>
    <v-form ref="form" v-model="valid" lazy-validation>
      <v-container>
        <v-row>
          <v-col>
            <v-select
              v-model="select"
              :items="items"
              :rules="[(v) => !!v || 'Item is required']"
              label="Item"
              required
            ></v-select
          ></v-col>

          <v-col>
            <v-text-field v-model="qty" label="Quantity" required></v-text-field
          ></v-col>

          <v-col>
            <v-text-field
              v-model="value"
              label="Offer Value"
              required
            ></v-text-field
          ></v-col>
        </v-row>

        <v-row>
          <v-col>
            <v-text-field
              v-model="tradeuid"
              label="Trade UID"
              outlined
              readonly
            ></v-text-field
          ></v-col>

          <v-col>
            <v-text-field
              v-model="hash"
              label="Trade Hash"
              outlined
              readonly
            ></v-text-field
          ></v-col>
        </v-row>
        <v-row>
          <v-btn
            :disabled="!valid"
            color="success"
            class="mr-4"
            @click="submit"
          >
            Submit
          </v-btn>

          <v-btn color="error" class="mr-4" @click="reset"> Reset Form </v-btn>
        </v-row>
      </v-container>
    </v-form>
  </v-container>
</template>

<script>
import axios from "axios";
import { nanoid } from "nanoid";
import hash from '@rakered/hash';

const APIHOST = "http://localhost/api/tradeengine";

export default {
  components: {},
  name: "SubmitTradeView",

  data() {
    return {
      select: null,
      items: ["Pineapples", "Apples", "Tomatoes", "Organges"],
      qty: 0,
      value: 0,
      trades: [],
      loading: true,
      options: {},
      valid:true,
      tradeuid: nanoid(),
    };
  },
  watch: {},
  methods: {
    async submit(e) {
      console.log(e)
      let offer = { "tradeId":this.tradeuid, "price":this.value,"qty":this.qty,"description":this.select};

      console.log(this.select)
      await axios.post(`${APIHOST}/trader/bob/trade`,offer);
      console.log(`Offer sent ${JSON.stringify(offer)}`);
      this.$router.push({ path: `/trades` });
    },
    reset() {
      this.$refs.form.reset();
    },
    async readDataFromAPI() {
      this.loading = true;
      await axios.get(`${APIHOST}/trades`).then((response) => {
        this.loading = false;
        this.trades = response.data;
      });
    },
  },
  mounted() {
   
  },
  asyncComputed: {
     hash: async function(){

        let message = `TradeMessage:${this.tradeuid}:OFFER:${this.qty}:${this.value}:${this.select}:`
        // let hashValue = await sha256(message);
        let { digest } = hash(message);
        console.log(`Hashing ${message} to ${digest}`);
        return digest;
    }
  }
};
</script>
