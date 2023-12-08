;; # Visualization

(ns visualization
  (:require [tablecloth.api :as tc]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.noj.v1.vis.hanami.templates :as vht]
            [scicloj.noj.v1.vis :as vis]
            [scicloj.noj.v1.stats :as stats]
            [scicloj.noj.v1.datasets :as datasets]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]
            [hiccup.core :as hiccup]
            [clojure2d.color :as color]))


;; ## Visualizing datases with Hanami

;; Noj offers a few convenience functions to make [Hanami](https://github.com/jsa-aerial/hanami) plotting work smoothly with [Tablecloth](https://scicloj.github.io/tablecloth/) and [Kindly](https://scicloj.github.io/kindly/).



(def random-walk
  (let [n 20]
    (-> {:x (range n)
         :y (->> (repeatedly n #(- (rand) 0.5))
                 (reductions +))}
        tc/dataset)))

;; ### A simple plot

;; We can plot a Tablecloth datasete using a Hanami template:

(-> random-walk
    (vis/hanami-plot ht/point-chart
                     {:MSIZE 200}))

;; Let us look inside the resulting vega-lite space. We can see the dataset is included as CSV:

(-> random-walk
    (vis/hanami-plot ht/point-chart
                     {:MSIZE 200})
    kind/pprint)

;; We can used Hanami temples from the namespace `[aerial.hanami.templates :as ht]'
;; as well as the additional templates at Noj's `[scicloj.noj.v1.vis.hanami.templates :as vht]`.

(-> datasets/iris
    (vis/hanami-plot vht/rule-chart
                     {:X :sepal-width
                      :Y :sepal-length
                      :X2 :petal-width
                      :Y2 :petal-length
                      :OPACITY 0.2
                      :SIZE 3
                      :COLOR "species"}))

;; ### Grouped datasets

;; Grouped datasets are handled automatically with a table view.

(-> datasets/iris
    (tc/group-by [:species])
    (vis/hanami-plot vht/rule-chart
                     {:X :sepal-width
                      :Y :sepal-length
                      :X2 :petal-width
                      :Y2 :petal-length
                      :OPACITY 0.2
                      :SIZE 3}))

;; ### Additional Hanami templates

;; The `scicloj.noj.v1.vis.hanami.templates` namespace add Hanami templates to Hanami's own collection.

(-> datasets/mtcars
    (vis/hanami-plot vht/boxplot-chart
                     {:X :gear
                      :XTYPE :nominal
                      :Y :mpg}))

;; ### Layers

(-> random-walk
    (vis/hanami-layers
     {:TITLE "points and a line"}
     [(vis/hanami-plot nil
                       ht/point-chart
                       {:MSIZE 400})
      (vis/hanami-plot nil
                       ht/line-chart
                       {:MSIZE 4
                        :MCOLOR "brown"})]))

;; Alternatively:

(-> random-walk
    (vis/hanami-combined-plot
     ht/layer-chart
     {:TITLE "points and a line"}
     :LAYER [[nil
              ht/point-chart
              {:MSIZE 400}]
             [nil
              ht/line-chart
              {:MSIZE 4
               :MCOLOR "brown"}]]))

;; ### Concatenation

;; Vertical

(-> random-walk
    (vis/hanami-vconcat
     {}
     [(vis/hanami-plot nil
                       ht/point-chart
                       {:MSIZE 400
                        :HEIGHT 100
                        :WIDTH 100})
      (vis/hanami-plot nil
                       ht/line-chart
                       {:MSIZE 4
                        :MCOLOR "brown"
                        :HEIGHT 100
                        :WIDTH 100})]))

;; Alternatively:

(-> random-walk
    (vis/hanami-combined-plot
     ht/vconcat-chart
     {:HEIGHT 100
      :WIDTH 100}
     :VCONCAT [[ht/point-chart
                {:MSIZE 400}]
               [ht/line-chart
                {:MSIZE 4
                 :MCOLOR "brown"}]]))

;; Horizontal

(-> random-walk
    (vis/hanami-hconcat
     {}
     [(vis/hanami-plot nil
                       ht/point-chart
                       {:MSIZE 400
                        :HEIGHT 100
                        :WIDTH 100})
      (vis/hanami-plot nil
                       ht/line-chart
                       {:MSIZE 4
                        :MCOLOR "brown"
                        :HEIGHT 100
                        :WIDTH 100})]))

;; Alternatively:
(-> random-walk
    (vis/hanami-combined-plot
     ht/hconcat-chart
     {:HEIGHT 100
      :WIDTH 100}
     :HCONCAT [[ht/point-chart
                {:MSIZE 400}]
               [ht/line-chart
                {:MSIZE 4
                 :MCOLOR "brown"}]]))

;; ### Linear regression

(-> datasets/mtcars
    (stats/add-predictions :mpg [:wt]
                           {:model-type :smile.regression/ordinary-least-square})
    (vis/hanami-combined-plot
     ht/layer-chart
     {:X :wt
      :MSIZE 200
      :HEIGHT 200}
     :LAYER [[ht/point-chart
              {:Y :mpg
               :WIDTH 200}]
             [ht/line-chart
              {:Y :mpg-prediction
               :MSIZE 5
               :MCOLOR "purple"
               :YTITLE :mpg}]]))

;; ### Histogram

(-> datasets/iris
    (vis/hanami-histogram :sepal-width
                          {:nbins 10}))

;; ### Combining a few things together
;;
;; The following is inspired by the example at Plotnine's [main page](https://plotnine.readthedocs.io/en/stable/).
;; Note how we add regression lines here. We take care of layout and colouring on our side, not using Vega-Lite for that.


(let [pallete (->> :accent
                   color/palette
                   (mapv color/format-hex))]
  (-> datasets/mtcars
      (tc/group-by :gear {:result-type :as-map})
      (->> (sort-by key)
           (map-indexed
            (fn [i [group-name ds]]
              (-> ds
                  (stats/add-predictions :mpg [:wt]
                                         {:model-type :smile.regression/ordinary-least-square})
                  (tc/select-columns [:gear :wt :mpg :mpg-prediction])
                  (vis/hanami-combined-plot
                   ht/layer-chart
                   {:TITLE (str "grear=" group-name)
                    :X :wt
                    :MCOLOR (pallete i)
                    :HEIGHT 200
                    :WIDTH 200}
                   :LAYER [[ht/point-chart
                            {:Y :mpg
                             :MSIZE 200}]
                           [ht/line-chart
                            {:Y :mpg-prediction
                             :MSIZE 5
                             :YTITLE :mpg}]]))))
           (vis/hanami-vconcat nil {}))))

;; A similar example with histograms:

(let [pallete (->> :accent
                   color/palette
                   (mapv color/format-hex))]
  (-> datasets/iris
      (tc/group-by :species {:result-type :as-map})
      (->> (sort-by key)
           (map-indexed
            (fn [i [group-name ds]]
              (-> ds
                  (vis/hanami-histogram :sepal-width
                                        {:nbins 10}))))
           (vis/hanami-vconcat nil {}))))

;; Scatterplots and regression lines again, this time using Vega-Lite for layout and coloring (using its "facet" option).

(-> datasets/mtcars
    (tc/group-by [:gear])
    (stats/add-predictions :mpg [:wt]
                           {:model-type :smile.regression/ordinary-least-square})
    (tc/ungroup)
    (tc/select-columns [:gear :wt :mpg :mpg-prediction])
    (vis/hanami-combined-plot
     ht/layer-chart
     {}
     :LAYER [[ht/point-chart
              {:X :wt
               :Y :mpg
               :MSIZE 200
               :COLOR "gear"
               :HEIGHT 100
               :WIDTH 200}]
             [ht/line-chart
              {:X :wt
               :Y :mpg-prediction
               :MSIZE 5
               :COLOR "gear"
               :YTITLE :mpg}]])
    ((fn [spec]
       {:facet {:row {:field "gear"}}
        :spec (dissoc spec :data)
        :data (:data spec)}))
    kind/vega-lite)


:bye
