include("./CFlow.l");

println("Preencha algumas informações");

ret = (v) -> {
	launch("ret", v);
};

cap = (v) -> {
	finalize("ret", v);
};

read = (message) -> {
    print(message);
    line = readln();
    ret(line);
};

name = cap(read("Nome: "));
age = cap(read("Idade: "));

println();

try: {
	assert(name != "", "O nome não foi preenchido");
} :: (err) -> {
	println(err);
};

try: {
	assert(age != "", "A idade não foi preenchida");
} :: (err) -> {
	println(err);
};

println();
println("Completo");

println();




println();
println("Name: " + name);
println("Age: " + age);






