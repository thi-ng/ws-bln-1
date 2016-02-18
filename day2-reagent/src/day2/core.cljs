(ns day2.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs.core.async.macros :refer [go go-loop]]
   [cljs-log.core :refer [debug info warn]])
  (:require
    [reagent.core :as reagent]))

(defn app-component
  []
  [:div [:h1 "hello"]])

(defn main
  []
  (reagent/render-component
    [app-component]
    (.-body js/document)))
