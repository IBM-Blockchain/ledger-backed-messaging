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

const APIHOST = "http://172.20.105.165:9090/v1";

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
      hash: "0xcafebabe",
      tradeuid: nanoid(),
    };
  },
  watch: {},
  methods: {
    submit(e) {
      console.log(e)
      // this.$router.push({ path: `/ledgerable/${e.tradeId}` });
    },
    reset() {
      this.$refs.form.reset();
    },
    readDataFromAPI() {
      this.loading = true;
      axios.get(`${APIHOST}/trades`).then((response) => {
        this.loading = false;
        this.trades = response.data;
      });
    },
  },
  mounted() {
    // this.readDataFromAPI();
  },
};
</script>
