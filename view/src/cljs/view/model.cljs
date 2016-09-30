(ns view.model
  (:require [clojure.string :as str]))

(defn load-json-file [file]
  (let [fs (js/require "fs")]
    (js->clj
     (->> file (.readFileSync fs) js/JSON.parse)
     :keywordize-keys true)))

(defn prettify-label
  "Extract the simple ClassLoader type name kept between the last dot and @."
  [label]
  (subs label
        (inc (str/last-index-of label "."))
        (str/last-index-of label "@")))

(defn some-includes? [filter classPath]
  (some #(str/includes? % filter) classPath))

(defn has-class-path-including? [name {:keys [classPath]}]
  (some-includes? filter classPath))

(def node-styles {:empty {:color "#E4EEFB"
                          :font {:color "#5a5a5a"}}

                  :has-files {:color "#CFDFF5"
                              :font {:color "#4a4a4a"}}

                  :has-matching-files {:color {:background "#5374A1"
                                               :border "#10315E"
                                               :highlight {:background "#214577" :border "#10315E"}}
                                       :font {:color "#faeaea"}}

                  :scope {:color "#FFEFC0"
                          :font {:color "#5a5a5a"}}})

(defn class-loader-style [{:keys [classPath]} filter]
  (if (empty? classPath)
    :empty
    (if (and (not (str/blank? filter))
             (some-includes? filter classPath))
      :has-matching-files
      :has-files)))

(defn class-loader-nodes [data filter]
  (->> data
       :classLoaders
       (map
        #(-> %
             (update :label prettify-label)
             (merge (get node-styles (class-loader-style % filter)))))))

(defn edges-for-node [{:keys [id parents]}]
  (map #(do {:from id :to % :arrows "to"}) parents))

(defn class-loader-edges [nodes]
  (mapcat edges-for-node nodes))

(defn scope-nodes [data]
  (->> data
       :scopes
       (map-indexed
        (fn [id scope]
          (-> scope
              (assoc :id id :shape "box")
              (merge (:scope node-styles)))))))

(defn scope-hierarchy-edges
  "Compute the edges connecting each scope to its parent."
  [nodes]
  (->> nodes
       butlast
       (map
        (fn [{:keys [id]}]
          {:from id :to (inc id) :arrows "to"}))))

(defn scope-loader-edges [nodes]
  (->> nodes
       (mapcat
        (fn [{:keys [id localClassLoader exportClassLoader]}]
          [{:from id :to localClassLoader :label "local"}
           {:from id :to exportClassLoader :label "export"}]))
       (map
        #(assoc % :arrows "to" :dashes true :font {:align "middle"}))))

(defn scope-edges [nodes]
  (concat (scope-hierarchy-edges nodes)
          (scope-loader-edges nodes)))

(defn concatv [& colls]
  (vec (apply concat colls)))

(defn ->network-data [class-loader-data filter]
  (let [nodes (class-loader-nodes class-loader-data filter)
        edges (class-loader-edges nodes)
        s-nodes (scope-nodes class-loader-data)
        s-edges (scope-edges s-nodes)]
    {:nodes (concatv nodes s-nodes)
     :edges (concatv edges s-edges)}))

(defn filter-class-loaders [model name]
  (let [pattern (str/trim name)]
    (if (str/blank? name)
      model
      (update model :classLoaders
              (fn [item] (filterv #(has-class-path-including? name %) item))))))
