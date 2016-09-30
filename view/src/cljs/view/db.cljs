(ns view.db
  (:require [clojure.string :as str]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [view.model :as model]))

(def default-db
  {:filter ""
   :model nil})

(re-frame/reg-event-db
 :initialize-db
 (fn [_ _]
   default-db))

(re-frame/reg-event-db
 :update-model
 (fn [db [_ model]]
   {:filter ""
    :model model}))

(re-frame/reg-event-db
 :update-filter
 (fn [db [_ filter]]
   (assoc db :filter filter)))

(defn get-node [db node-id]
  (->>
   (get-in db [:model :classLoaders])
   (filter #(= node-id (:id %)))
   first))

(re-frame/reg-event-db
 :select-node
 (fn [db [_ node-id]]
   (let [selected (get-node db node-id)]
     (assoc db :selected selected))))

(re-frame/reg-sub
 :filter
 (fn [db]
   (:filter db)))

(re-frame/reg-sub
 :model
 (fn [db]
   (:model db)))

(re-frame/reg-sub
 :classPath
 (fn [db]
   (-> db (get-in [:selected :classPath]) sort)))

(re-frame/reg-sub
 :network

 (fn [query-v _]
   [(subscribe [:filter])
    (subscribe [:model])])

 (fn [[filter model] _]
   (model/->network-data model (str/trim filter))))
