(ns view.core
  (:require [clojure.string :as str]
            [goog.events :as ev]
            [reagent.core :as r]
            [reagent.ratom :refer [reaction]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-com.core :as re-com]
            [devtools.core :as devtools]
            [view.config :as config]
            [view.model :as model]
            [view.db :as db]
            [cljsjs.vis]))

(def Network js/vis.Network)
(def DataSet js/vis.DataSet)

(defn group-selector-for [filter]
  (if (str/blank? filter)
    #(do :il.list-group-item)
    #(if (str/includes? % filter)
      :il.list-group-item.list-group-item-info
      :il.list-group-item)))

(defn selected-classPath []
  (let [items (subscribe [:classPath])
        filter (subscribe [:filter])]
    (fn []
      (let [group-selector-fn (group-selector-for @filter)]
        [:ul.list-group
         (map
          #(do [(group-selector-fn %) {:key % :style {:text-align :left}} %])
          @items)]))))

(defn filter-input []
  (let [filter (subscribe [:filter])]
    (fn []
      [re-com/h-box
       :justify :center
       :align :stretch
       :children [[re-com/input-text
                   :model filter
                   :width "100%"
                   :placeholder "Search for ClassLoaders containing a particular file"
                   :change-on-blur? false
                   :on-change (fn [v] (dispatch [:update-filter v]))]]])))

(defn maximize [network]
  (.setSize network (str js/window.innerWidth) (str js/window.innerHeight)))

(defn network []
  (let [network (atom nil)
        options {:autoResize false}
        update (fn [component]
                 (let [{:keys [nodes edges]} (r/props component)]
                   (doto @network
                     (maximize)
                     (.setData (clj->js {:nodes nodes :edges edges})))))
        on-resize (fn []
                    (doto @network
                      maximize
                      .redraw))
        resize-listener-key (atom nil)
        setup-resize-listener (fn []
                                (reset! resize-listener-key
                                        (ev/listen js/window
                                                   (.-RESIZE ev/EventType)
                                                   on-resize)))]
    (r/create-class
     {:reagent-render (fn []
                        [:div#network])

      :component-did-mount (fn [component]
                             (let [container (r/dom-node component)
                                   {:keys [on-click]} (r/props component)
                                   network' (Network. container #js {} (clj->js options))]
                               (when on-click
                                 (.on network' "click" on-click))
                               (reset! network network'))
                             (setup-resize-listener)
                             (update component))

      :component-will-unmount (fn [component]
                                (ev/unlistenByKey @resize-listener-key)
                                (.destroy @network)
                                (reset! network nil))

      :component-did-update update
      :display-name "network-class"})))

(defn on-click-node [click]
  (let [node-id (-> click .-nodes first)]
    (dispatch [:select-node node-id])))

(defn network-container []
  (let [data (subscribe [:network])]
    (fn []
      [network (assoc @data :on-click on-click-node)])))

(defn left-panel []
  [re-com/box
   :align :stretch
   :size "1 1 100"
   :child [network-container]])

(defn right-panel []
  [re-com/box
   :size "1 1 100"
   :align :stretch
   :child [selected-classPath]])

(defn main-panel []
  (fn []
    [re-com/v-box
     :align :stretch
     :padding "2px"
     :gap "2px"
     :size "1"
     :children [[filter-input]
                [re-com/h-split
                 :height "100%"
                 :width "100%"
                 :size "1 0 100"
                 :initial-split 70
                 :margin "0px"
                 :panel-1 [left-panel]
                 :panel-2 [right-panel]]]]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")
    (devtools/install!)))

(defn mount-root []
  (r/render [main-panel]
            (.getElementById js/document "app")))

(def electron (js/require "electron"))
(def ipcRenderer (.-ipcRenderer electron))
(def remote-app (-> electron .-remote .-app))

(defn on-open-file [event path]
  (re-frame/dispatch [:update-model (model/load-json-file path)])
  (.addRecentDocument remote-app path))

(defn setup-open-file-handler []
  (.on ipcRenderer "open-file" on-open-file))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (setup-open-file-handler)
  (dev-setup)
  (mount-root))
