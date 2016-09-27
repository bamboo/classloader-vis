(ns main.core)

(defonce app-state (atom nil))

(def electron       (js/require "electron"))
(def app            (.-app electron))
(def browser-window (.-BrowserWindow electron))
(def Menu           (.-Menu electron))
(def dialog         (.-dialog electron))

(defn on-open-file [path]
  (some-> @app-state :main-window .-webContents (.send "open-file" path)))

(defn on-open-file-menu []
  (let [files (.showOpenDialog dialog #js {"properties" #js ["openFile"]})]
    (some-> files first on-open-file)))

(defn setup-menu []
  (let [template (clj->js [{:label (.getName app)
                            :submenu [{:role "about"}
                                      {:type "separator"}
                                      {:role "services" :submenu []}
                                      {:type "separator"}
                                      {:role "hide"}
                                      {:role "hideothers"}
                                      {:role "unhide"}
                                      {:type "separator"}
                                      {:role "quit"}]}
                           {:label "File"
                            :submenu [{:label "Open..."
                                       :accelerator "CmdOrCtrl+O"
                                       :click (fn [item focused-window] (on-open-file-menu))}]}
                           {:label "View"
                            :submenu [{:label "Reload"
                                       :accelerator "Command+R"
                                       :click (fn [item focused-window] (-> focused-window .reload))}
                                      {:type "separator"}
                                      {:label "Toggle Developer Tools"
                                       :accelerator "Alt+Command+I"
                                       :click (fn [item focused-window] (-> focused-window .-webContents .toggleDevTools))}
                                      {:type "separator"}
                                      {:role "togglefullscreen"}]}
                           {:label "Window"
                            :submenu [{:label "Close"
                                       :accelerator "CmdOrCtrl+W"
                                       :role "close"}
                                      {:label "Minimize"
                                       :accelerator "CmdOrCtrl+M"
                                       :role "minimize"}
                                      {:label "Zoom"
                                       :role "zoom"}
                                      {:type "separator"}
                                      {:label "Bring All to Front"
                                       :role "front"}]}])]
    (->> template
         (.buildFromTemplate Menu)
         (.setApplicationMenu Menu))))

(defn make-window
  "Creates a top level browser window."
  []
  (browser-window. (clj->js {:width 800 :height 600})))

(defn make-child [parent url]
  (doto (browser-window. (clj->js {:parent parent :modal true}))
    (.loadURL url)))

(defn load-clojure [window]
  (.loadURL window "http://clojure.org"))

(defn home [window]
  (.loadURL window (str "file:///" (.getAppPath app) "/../view/resources/public/index.html")))

(defn on-app-ready []
  (let [main-window (make-window)]
    (reset! app-state {:main-window main-window})
    (setup-menu)
    (home main-window)))

(defn on-app-window-all-closed []
  (.quit app))

(defn on-app-will-finish-launching []
  (.on app
       "open-file"
       (fn [event path]
         (.preventDefault event)
         (on-open-file path))))

;; do nothing entry point
(defn- main []
  (enable-console-print!)
  (println "This text is printed from src/main/core.cljs. Go ahead and edit it and see reloading in action.")

  (.on app "will-finish-launching" on-app-will-finish-launching)
  (.on app "ready" on-app-ready)
  (.on app "window-all-closed" on-app-window-all-closed))

(set! *main-cli-fn* main)
