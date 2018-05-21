(defproject dactyl-keyboard "0.2.0-SNAPSHOT"
  :description "A parametrized, split-hand, concave, columnar, erogonomic keyboard"
  :url "http://viktor.eikman.se/article/the-dmote/"
  :main dactyl-keyboard.dactyl
  :license {:name "GNU Affero General Public License"
            :url "https://www.gnu.org/licenses/agpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.7"]
                 [io.forward/yaml "1.0.8"]
                 [unicode-math "0.2.0"]
                 [scad-clj "0.5.2"]])
