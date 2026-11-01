import os

test_folder = "tests"
program_name = "bin/./tpcc"

result = 0
echec = 0

# List all directories first
directories = [d for d in os.listdir(test_folder) if os.path.isdir(os.path.join(test_folder, d))]

for dir_name in directories:
    dir_path = os.path.join(test_folder, dir_name)
    
    if dir_name == "good":
        print(f'\033[01;33mTest correct :\033[01;00m')
    elif dir_name == "syn-err":
        print(f'\033[01;33mTest erreur syntaxique :\033[01;00m')
    elif dir_name == "sem-err":
        print(f'\033[01;33mTest erreur sémantique :\033[01;00m')
    elif dir_name == "warn":
        print(f'\033[01;33mTest avertissement :\033[01;00m')
    else:
        continue  # Skip directories that are not in the specified list

    for filename in os.listdir(dir_path):
        file_path = os.path.join(dir_path, filename)
        if os.path.isfile(file_path):
            command = f"{program_name} < {file_path}"
            result_test = os.system(command)
            
            if dir_name == "syn-err":
                if result_test != 0:
                    result += 1
                else:
                    print(f"{filename} : devrait renvoyer une erreur")
                    echec += 1
            elif dir_name == "sem-err":
                if result_test != 0:
                    result += 1
                else:
                    print(f"{filename} : devrait renvoyer une erreur")
                    echec += 1
            elif dir_name == "warn":
                if result_test == 0:
                    result += 1
                else:
                    print(f"Test échoué pour {filename}")
                    echec += 1
            elif dir_name == "good":
                if result_test == 0:
                    result += 1
                else:
                    print(f"Test échoué pour {filename}")
                    echec += 1

print("+--------------------------------+")
print("+   " + str(result + echec) + " tests run/" + str(result) + " tests passed  +")
print("+--------------------------------+")