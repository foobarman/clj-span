;;; Copyright 2010 Gary Johnson
;;;
;;; This file is part of clj-span.
;;;
;;; clj-span is free software: you can redistribute it and/or modify
;;; it under the terms of the GNU General Public License as published
;;; by the Free Software Foundation, either version 3 of the License,
;;; or (at your option) any later version.
;;;
;;; clj-span is distributed in the hope that it will be useful, but
;;; WITHOUT ANY WARRANTY; without even the implied warranty of
;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;;; General Public License for more details.
;;;
;;; You should have received a copy of the GNU General Public License
;;; along with clj-span.  If not, see <http://www.gnu.org/licenses/>.
;;;
;;;-------------------------------------------------------------------
;;;
;;; This namespace defines the text-based menu interface for viewing
;;; the results of a SPAN model run as well as the raw data return
;;; functions.

(ns clj-span.interface
  (:use	[clj-misc.utils      :only (mapmap)]
	[clj-misc.randvars   :only (rv-zero)]
	[clj-misc.matrix-ops :only (matrix2seq coord-map2matrix print-matrix get-rows get-cols in-bounds?)]))

(defn- select-location
  "Prompts for coords and returns the selected [i j] pair."
  [rows cols]
  (loop []
    (printf "%nInput location coords%n")
    (let [coords [(do (printf "Row [0-%d]: " (dec rows)) (flush) (read))
		  (do (printf "Col [0-%d]: " (dec cols)) (flush) (read))]]
      (if (in-bounds? rows cols coords)
	coords
	(do (printf "No location at %s. Enter another selection.%n" coords)
	    (recur))))))

(defn- view-location-properties
  "Prints a summary of the post-simulation properties of the
   location."
  [coords source-layer sink-layer use-layer flow-layers]
  (let [fmt-str (str
		 "%nLocation %s%n"
		 "--------------------%n"
		 "Source:        %s%n"
		 "Sink:          %s%n"
		 "Use:           %s%n"
		 "Flow Features: %s%n")]
    (printf fmt-str
	    coords
	    (get-in source-layer coords)
	    (get-in sink-layer   coords)
	    (get-in use-layer    coords)
	    (mapmap identity #(get-in % coords) flow-layers))))

(defn- select-menu-option
  "Prompts the user with a menu of choices and returns the label
   corresponding to their selection."
  [prompt-list]
  (let [prompts       (vec prompt-list)
	num-prompts   (count prompts)
	index-padding (count (str num-prompts))]
    (loop []
      (printf "%nOptions Menu:%n")
      (dotimes [i num-prompts]
	(printf (str " %" index-padding "d) %s%n") (inc i) (prompts i)))
      (print "Choice: ")
      (flush)
      (let [choice (read)]
	(if (and (integer? choice) (> choice 0) (<= choice num-prompts))
	  (prompts (dec choice))
	  (do (println "Invalid selection. Please choose a number from the menu.")
	      (recur)))))))

(defn- select-map-by-feature
  "Prompts for a feature available in the union of the source, sink,
   use, and flow layers, and returns a map of {[i j] -> value} for the
   one selected, where value is either a double or a probability
   distribution."
  [source-layer sink-layer use-layer flow-layers]
  (let [feature-names    (list* "Source" "Sink" "Use" (keys flow-layers))
	selected-feature (select-menu-option feature-names)
	selected-layer   ((-> flow-layers
			      (assoc "Source" source-layer)
			      (assoc "Sink"   sink-layer)
			      (assoc "Use"    use-layer))
			  selected-feature)]
    selected-layer))
    ;;    (into {} (for [i (range (get-rows source-layer)) j (range (get-cols source-layer)) :let [id [i j]]]
;;	       [id (get-in selected-layer id)]))))

(defmulti provide-results (fn [result-type results-menu source-layer sink-layer use-layer flow-layers] result-type))

(defmethod provide-results :cli-menu
  [_ results-menu source-layer sink-layer use-layer flow-layers]
  (let [rows        (get-rows source-layer)
	cols        (get-cols source-layer)
	menu-extras (array-map
		     "Location Properties"
		     #(view-location-properties (select-location rows cols) source-layer sink-layer use-layer flow-layers)
		     "Input Features"
		     #(select-map-by-feature source-layer sink-layer use-layer flow-layers)
		     "Quit"
		     nil)
	menu        (apply array-map (apply concat (concat results-menu menu-extras)))
	prompts     (keys menu)]
    (loop [action (menu (select-menu-option prompts))]
      (when action
	(when-let [matrix-result (action)]
;;	(when-let [coord-map (action)]
	  (newline)
	  (print-matrix matrix-result)
;;	  (print-matrix (coord-map2matrix rows cols rv-zero coord-map))
	  (newline)
	  (println "Distinct values:" (count (distinct (matrix2seq matrix-result)))))
;;	  (println "Distinct values:" (count (distinct (vals coord-map)))))
	(recur (menu (select-menu-option prompts)))))))

;; FIXME: The results-menu actions generate matrices, not coord-maps.
(defmethod provide-results :closure-map
  [_ results-menu _ _ _ _]
  results-menu)

;; FIXME: The results-menu actions generate matrices, not coord-maps.
(defmethod provide-results :matrix-map
  [_ results-menu source-layer _ _ _]
  (let [rows (get-rows source-layer)
	cols (get-cols source-layer)]
    (mapmap identity #(coord-map2matrix rows cols rv-zero (%)) results-menu)))
