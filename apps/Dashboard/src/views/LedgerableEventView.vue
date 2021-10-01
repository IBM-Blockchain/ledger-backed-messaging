<template>
  <v-container>
    <h1 style="text-align: center">
      Ledgerable Events for Trade {{ $route.params.id }}
    </h1>

    <v-data-table
      :page="page"
      :pageCount="numberOfPages"
      :headers="headers"
      :items="ledgerables"
      :loading="loading"
      class="elevation-1"
    >
      <template v-slot:[`item.logs`]="{ item }">
        <ul>
          <li v-for="l in item.logs" :key="l.timestamp">
            <h3>{{ l.type }}</h3>
            <p>
            [{{ l.timestamp | fromMilliSeconds }}]
            
            {{ l.dataHash }}
            </p>

          </li>
        </ul>
      </template>
    </v-data-table>
  </v-container>
</template>

<script>
import axios from "axios";

const APIHOST = "https://localhost/api/ledger";
export default {
  name: "LedgerableEventView",

  data() {
    return {
      page: 1,
      totalPassengers: 0,
      numberOfPages: 0,
      ledgerables: [],
      loading: true,
      options: {},
      headers: [
        { text: "TradeId", value: "eventId" },
        // { text: "SubId", value: "subId" },
        { text: "Logs", value: "logs" },
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
    readDataFromAPI() {
      this.loading = true;
      let trade = this.$route.params.id;
      axios.get(`${APIHOST}/event/${trade}`).then((response) => {
        console.log(response.data);
        this.loading = false;
        this.ledgerables = response.data;
        this.numberOfPages = 1;
      });
    },
  },
  mounted() {
    this.readDataFromAPI();
  },
  filters: {
    fromMilliSeconds(t){
      console.log(t)
      return new Date(t*1000).toString()
    }
  }
};
</script>
