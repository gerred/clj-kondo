(ns clj-kondo.impl.inactive-config-test
  (:require [babashka.fs :as fs]
            [clj-kondo.impl.core :as core-impl]
            [clj-kondo.impl.utils :as utils]
            [clojure.test :refer [deftest is testing]]))

(deftest inactive-import-configs-test
  (testing "no findings"
    (testing "when clj-kondo :config-dir is not present"
      (is (nil? (core-impl/inactive-config-imports {:detected-configs (atom ["cfg1" "cfg2"])
                                                    :config {:config-paths ["cfg3" "cfg4"]}}))))
    (testing "when clj-kondo :config-paths already specify :detected-configs"
      (is (nil? (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                    :detected-configs (atom ["cfg1" "cfg2"])
                                                    :config {:config-paths ["cfg1" "cfg2"]}})))))
  (testing "when there were no :detected-configs"
    (is (nil? (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                  :detected-configs (atom [])
                                                  :config {:config-paths ["cfg1" "cfg2"]}}))))
  (testing "findings when there are :detected-configs"
    (testing "and clj-kondo config has no :config-paths"
      (is (= [{:imported-config "cfg-dir/cfg1" :suggested-config-path "\"cfg1\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg2" :suggested-config-path "\"cfg2\"" :config-file "cfg-dir/config.edn"}]
             (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                 :detected-configs (atom ["cfg1" "cfg2"])
                                                 :config {}}))))
    (testing "and clj-kondo :config-paths is empty"
      (is (= [{:imported-config "cfg-dir/cfg1" :suggested-config-path "\"cfg1\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg2" :suggested-config-path "\"cfg2\"" :config-file "cfg-dir/config.edn"}]
             (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                 :detected-configs (atom ["cfg1" "cfg2"])
                                                 :config {:config-paths []}}))))
    (testing "and clj-kondo :config-paths does not overlap with :detected-configs"
      (is (= [{:imported-config "cfg-dir/cfg1" :suggested-config-path "\"cfg1\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg2" :suggested-config-path "\"cfg2\"" :config-file "cfg-dir/config.edn"}]
             (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                 :detected-configs (atom ["cfg1" "cfg2"])
                                                 :config {:config-paths ["cfg4" "cfg5"]}}))))
    (testing "and clj-kondo :config-paths has some overlap with :detected-configs"
      (is (= [{:imported-config "cfg-dir/cfg1" :suggested-config-path "\"cfg1\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg2" :suggested-config-path "\"cfg2\"" :config-file "cfg-dir/config.edn"}]
             (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                 :detected-configs (atom ["cfg1" "cfg2" "cfg3" "cfg6"])
                                                 :config {:config-paths ["cfg3" "cfg4" "cfg5" "cfg6"]}}))))
    (testing "are sorted"
      (is (= [{:imported-config "cfg-dir/cfg/b" :suggested-config-path "\"cfg/b\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg/c" :suggested-config-path "\"cfg/c\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg/w" :suggested-config-path "\"cfg/w\"" :config-file "cfg-dir/config.edn"}
              {:imported-config "cfg-dir/cfg/z" :suggested-config-path "\"cfg/z\"" :config-file "cfg-dir/config.edn"}]
             (core-impl/inactive-config-imports {:config-dir "cfg-dir"
                                                 :detected-configs (atom ["cfg/c" "cfg/z" "cfg/b" "cfg/w"])
                                                 :config {}}))))
    (testing "returns config-dir relative to current dir"
      (is (= [{:imported-config "cfg-dir/cfg1" :suggested-config-path "\"cfg1\"" :config-file "cfg-dir/config.edn"}]
             (core-impl/inactive-config-imports {:config-dir (str (fs/absolutize "cfg-dir"))
                                                 :detected-configs (atom ["cfg1"])
                                                 :config {}}))))
    (when utils/windows?
      (testing "unixifies all paths on Windows"
        (is (= [{:imported-config "cfg-dir/cfg/c" :suggested-config-path "\"cfg/c\"" :config-file "cfg-dir/config.edn"}
                {:imported-config "cfg-dir/cfg/z" :suggested-config-path "\"cfg/z\"" :config-file "cfg-dir/config.edn"}]
               (core-impl/inactive-config-imports {:config-dir (str (fs/absolutize "cfg-dir"))
                                                   :detected-configs (atom ["cfg\\c" "cfg\\z" "cfg\\b" "cfg\\w"])
                                                   :config {:config-paths ["cfg\\w" "cfg/b"]}})))))))


(deftest print-inactive-import-configs-test
  (is (= (str "Imported config to cfg/a/b. To activate, add \"a/b\" to :config-paths in cfg/config.edn." (System/getProperty "line.separator")
              "Imported config to cfg/c/d. To activate, add \"c/d\" to :config-paths in cfg/config.edn." (System/getProperty "line.separator"))
         (let [s (new java.io.StringWriter)]
           (binding [*err* s]
             (core-impl/print-inactive-config-imports
              [{:imported-config "cfg/a/b" :suggested-config-path "\"a/b\"" :config-file "cfg/config.edn"}
               {:imported-config "cfg/c/d" :suggested-config-path "\"c/d\"" :config-file "cfg/config.edn"}]))
           (str s)))))

