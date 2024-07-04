/**
 * Класс для разблокировки MacOS Keychain.
 *   при подключении по ssh системное хранилище ключей остается закрытым, для обхода
 *   используется временный keychain в каторый экспортируктся сертификат разработчика apple.
 *
 * подробнее описано тут: 
 *   1. https://developer.apple.com/forums/thread/712005
 */
class KeychainUnlocker implements Serializable {
    
    /**
     * Метод создания временного хранилища ключей
     *
     * @param script                    Script, req - Контекст DSL
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     * @param temp_keychain_pass        String, opt - пароль от временного хранилища ключей, default: 'PASSWORD'.
     */
    static void createTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        String temp_keychain_pass       = args.temp_keychain_pass ?: 'PASSWORD'

        args.script.println ">>> Temp keychain will be created."
        args.script.println "[debug] temp_keychain = ${temp_keychain}"
        args.script.println "[debug] temp_keychain_pass = ${temp_keychain_pass}"
                            
        args.script.sh "security create-keychain -p ${temp_keychain_pass} ${temp_keychain}"
    }

    /**
     * Для добавления в список временного keychain.
     *
     * @param script                    Script, req - Контекст DSL
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     */
    static void appendTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        
        args.script.println ">>> Append keychain to the search list."        
        args.script.println "[debug] temp_keychain = ${temp_keychain}"

        args.script.sh """security list-keychains -d user -s ${temp_keychain} \$(security list-keychains -d user | sed s/\\"//g)"""
        args.script.sh "security set-keychain-settings ${temp_keychain}"
    }  

    /**
     * Метод для разблокировки временного keychain.
     *
     * @param script                    Script, req - Контекст DSL
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     * @param temp_keychain_pass        String, opt - пароль от временного хранилища ключей, default: 'PASSWORD'.
     */
    static void unlockTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        String temp_keychain_pass       = args.temp_keychain_pass ?: 'PASSWORD'

        args.script.println ">>> Append keychain to the search list."        
        args.script.println "[debug] temp_keychain = ${temp_keychain}"
        args.script.println "[debug] temp_keychain_pass = ${temp_keychain_pass}"
                            
        args.script.sh "security unlock-keychain -p ${temp_keychain_pass} ${temp_keychain}"
    }  

    /**
     * Метод для импорта сертификата разработчика apple во временный keychain.
     *
     * @param script                    Script, req - Контекст DSL
     * @param cert_path                 Script, opt - путь до сертификата apple developer в формате .p12, default: '/Users/jenkins/developer.p12'.
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     * @param temp_keychain_pass        String, opt - пароль от временного хранилища ключей, default: 'PASSWORD'.
     */
    static void certImportTempKeychain(Map args = [:]) {
        String cert_path                = args.cert_path ?: '/Users/jenkins/developer.p12'
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        String temp_keychain_pass       = args.temp_keychain_pass ?: 'PASSWORD'

        args.script.println ">>> Import certificate to temp keychain"
        args.script.println "[debug] cert_path = ${cert_path}"
        args.script.println "[debug] temp_keychain = ${temp_keychain}"
        args.script.println "[debug] temp_keychain_pass = ${temp_keychain_pass}"
                   
        args.script.sh "security import ${cert_path} -k ${temp_keychain} -P ${temp_keychain_pass} -T '/usr/bin/codesign'"
    }

    /**
     * Метод отображения информации о разработчике apple.
     *
     * @param script                    Script, req - Контекст DSL
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     */
    static void infoDeveloperIdentity(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        
        args.script.println ">>> Detect the iOS identity."        
        args.script.println "[debug] temp_keychain = ${temp_keychain}"

        args.script.sh """
            IOS_IDENTITY=\$(security find-identity -v -p codesigning ${temp_keychain} | head -1 | grep '"' | sed -e 's/[^"]*"//' -e 's/".*//')
            IOS_UUID=\$(security find-identity -v -p codesigning ${temp_keychain} | head -1 | grep '"' | awk '{print \$2}')
            """
    } 

    /**
     * Установка метадаты на keychain.
     *
     * @param script                    Script, req - Контекст DSL
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     * @param temp_keychain_pass        String, opt - пароль от временного хранилища ключей, default: 'PASSWORD'.
     */
    static void setPartitionList(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        String temp_keychain_pass       = args.temp_keychain_pass ?: 'PASSWORD'

        args.script.println ">>> Setting new requirement for MacOS 10.12"        
        args.script.println "[debug] temp_keychain = ${temp_keychain}"
        args.script.println "[debug] temp_keychain_pass = ${temp_keychain_pass}"
                            
        args.script.sh "security set-key-partition-list -S apple-tool:,apple: -s -k ${temp_keychain_pass} ${temp_keychain}"
    }
    
    /**
     * Удаление временного keychain.
     *
     * @param script                    Script, req - Контекст DSL
     * @param temp_keychain             String, opt - имя временного хранилища ключей, default: 'temp_jenkins-ci.keychain'.
     */
    static void deleteTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        
        args.script.println ">>> Temp keychain will be deleted."       
        args.script.println "[debug] temp_keychain = ${temp_keychain}"

        args.script.sh "security delete-keychain ${temp_keychain}"
    }                 
    
    /**
     * Вывож списка всех keychains.
     *
     * @param script                    Script, req - Контекст DSL
     */
    static void listKeychains(Map args = [:]) {    
        args.script.sh "security list-keychains"
    }            
}
