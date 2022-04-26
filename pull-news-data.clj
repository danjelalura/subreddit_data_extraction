#!/usr/bin/env bb

(require '[babashka.curl :as curl]
         '[cheshire.core :as cheshire]
         '[clojure.java.io :as io]
         '[clojure.walk :as w])

(defn pull-data [url]
  (:body
   (curl/get url
             {:query-params {"limit" 100}
              :headers      {"User-agent" "your bot 0.1"}})))

(defn write-data [data file-name]
  (with-open [out (io/writer (io/file (str "news_data/" file-name)) :append true)]
    (spit out data)))

#_(defn append-data)

(defn read-json-data [file-name]
  (with-open [reader (io/reader (io/file (str "news_data/" file-name)))]
    (cheshire/parse-string
      (slurp reader))))

(let [[subreddit type-of-post] *command-line-args*]
  (try
    (do

     (println "Pulling news from data...")
     (write-data
       (pull-data
         (str "https://www.reddit.com/r/" subreddit "/" type-of-post ".json"))
       "news_data.json")

     (println "Saving the news to this machine...")
     (let [data  (-> "news_data.json"
                     read-json-data
                     w/keywordize-keys
                     :data
                     :children
                     (->> (map :data)))

             titles   (map :title data)
             dates    (map :created_utc data)
             news_data (map  (fn [title date]
                              {:title title :date date})
                             titles dates)]
       (write-data (apply str news_data)
                   "cleaned_news_data.edn")))

   (catch Exception e (str "Usage <subreddit> <type-of-post>" (.getMessage e)))))
