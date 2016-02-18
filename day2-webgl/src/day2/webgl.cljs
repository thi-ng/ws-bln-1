(ns day2.webgl
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.shaders.lambert :as lambert]
   [thi.ng.geom.webgl.shaders.phong :as phong]
   [thi.ng.geom.webgl.utils :as glu]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.matrix :as mat :refer [M44]]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.polygon :as poly]
   [thi.ng.geom.basicmesh :refer [basic-mesh]]
   [thi.ng.morphogen.core :as mg]
   [thi.ng.typedarrays.core :as arrays]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]))

(enable-console-print!)

(defn mg-hex-sphere
   []
   (let [hex           (mg/apply-recursively (mg/reflect :e) 5 [1] 1)
         reflected-hex (mg/reflect :n :out [{} hex])
         inject        #(-> hex
                            (assoc-in (mg/child-path [1 1 0]) %)
                            (assoc-in (mg/child-path [1 1 1 1 0]) %))
         seed-clone    (mg/reflect
                        :s :out [{} (inject reflected-hex)])
         tree          (mg/reflect
                        :s :out [(inject seed-clone) (inject reflected-hex)])]
     (-> (mg/seed-box (mg/sphere-lattice-seg 6 0.25 0.0955 0.2))
         (mg/generate-mesh tree)
         (g/transform (-> M44 (g/rotate-x (- HALF_PI)) (g/scale 0.5))))))

(defn ^:export cube-demo
  []
  (let [gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        model     (-> 
;                    (a/aabb [0 0 0] 0.75)
;                      (g/center)
;                      (g/as-mesh)
                      (mg-hex-sphere)
                      (gl/as-webgl-buffer-spec {})
                      (buf/make-attribute-buffers-in-spec gl gl/static-draw)
                      (assoc :shader (sh/make-shader-from-spec gl lambert/shader-spec))
                      (update-in [:uniforms] merge
                                 {:proj          (gl/perspective 45 view-rect 0.1 100.0)
                                  :view          (mat/look-at (vec3 0 0 2) (vec3) (vec3 0 1 0))
                                  :lightCol      (vec3 1 0 0)
                                  ;;:lightDir      (g/normalize (vec3 -1 1 1))
                                  :ambientCol    0x000066
                                  :diffuseCol    0xffffff
                                  }))]
    (anim/animate
     (fn [[t frame]]
       (let [tx (-> M44  ;; deg * PI / 180 -> rad * 180 / PI
                    (g/rotate-x (* t 0.5))
                    (g/rotate-y (* t 0.25)))]
         (gl/set-viewport gl view-rect)
         (gl/clear-color-buffer gl 1 0.9 0.9 0.9)
         (gl/clear-depth-buffer gl 1)
         (gl/enable gl gl/depth-test)
         (lambert/draw gl (assoc-in model [:uniforms :model] tx)))
       true))))

(defn ^:export gear-demo
  []
  (let [gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        teeth     20
        offset    0.25
        model     (-> (poly/cog 0.5 teeth [0.6 0.7 0.8 1.0 1.0 0.9])
                      (g/extrude-shell
                       {:mesh    (basic-mesh)
                        :depth   0.2
                        :inset   0.025
                        :wall    0.015
                        :bottom? true
                        :top? true})
                      (gl/as-webgl-buffer-spec {})
                      (buf/make-attribute-buffers-in-spec gl gl/static-draw)
                      (assoc :shader (sh/make-shader-from-spec gl phong/shader-spec))
                      (update-in [:uniforms] merge
                                 {:proj          (gl/perspective 45 view-rect 0.1 100.0)
                                  :lightPos      (vec3 0.1 0 1)
                                  :ambientCol    0x0e1a4c
                                  :diffuseCol    0xff3310
                                  :specularCol   0x99ffff
                                  :shininess     100
                                  :wrap          0
                                  :useBlinnPhong true})
                      (time))]
    (anim/animate
     (fn [[t frame]]
       (let [model (assoc-in
                    model [:uniforms :view]
                    (mat/look-at (vec3 0 0 2) (vec3) (vec3 0 1 0)))
             rot   (g/rotate-y M44 (* t 0.5))
             tx1   (g/* rot (-> M44
                                (g/translate (- offset) 0 0)
                                (g/rotate-y 0.3)
                                (g/rotate-z t)))
             tx2   (g/* rot (-> M44
                                (g/translate offset 0 0)
                                (g/rotate-y -0.3)
                                (g/rotate-z (- (+ t (/ HALF_PI teeth))))))]

         (gl/set-viewport gl view-rect)
         (gl/clear-color-buffer gl 1 0.9 0.9 0.9)
         (gl/clear-depth-buffer gl 1)
         (gl/enable gl gl/depth-test)
         (phong/draw gl (assoc-in model [:uniforms :model] tx1))
         (phong/draw
          gl (-> model
                 (assoc-in [:uniforms :model] tx2)
                 (assoc-in [:uniforms :diffuseCol] 0x33ff80)))
         true)))))

(cube-demo)
