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
;;; This namespace defines a number of functions for generating
;;; source, sink, use, and flow layers (randomly or from GIS maps),
;;; writing them to files, and reading them back.

(ns clj-span.worldgen
  (:use [clj-misc.matrix-ops :only (make-matrix)]
	[clj-misc.randvars   :only (cont-type disc-type)]
	[clojure.contrib.duck-streams :only (spit file-str with-in-reader read-lines)]))

(defn read-layer-from-file
  [filename]
  (with-in-reader (file-str filename) (read)))

(defn write-layer-to-file
  [filename layer]
  (binding [*print-dup* true]
    (spit (file-str filename) layer)))

(defn make-random-layer
  [rows cols type]
  {:pre [(#{:discrete :continuous} type)]}
  (let [meta (if (= type :discrete) disc-type cont-type)]
    (make-matrix rows cols #(with-meta {(rationalize (rand 100.0)) 1} meta))))

(defn make-random-layer-map
  [rows cols name-to-type-map]
  {:pre [(every? #{:discrete :continuous} (vals name-to-type-map))]}
  (into {}
	(for [[name type] name-to-type-map]
	  [name (let [meta (if (= type :discrete) disc-type cont-type)]
		  (make-matrix rows cols #(with-meta {(rationalize (rand 100.0)) 1} meta)))])))

(defn make-layer-from-ascii-grid
  [filename]
  (let [lines (read-lines filename)
	rows  (Integer/parseInt (second (re-find #"^NROWS\s+(\d+)" (first  lines))))
	cols  (Integer/parseInt (second (re-find #"^NCOLS\s+(\d+)" (second lines))))
	data  (drop-while #(re-find #"^[^\d]" %) lines)]
    (println "Stub...process the data...")))