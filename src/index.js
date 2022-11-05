import "./styles.css";

if (document.readyState !== "loading") {
  initializeCode();
} else {
  document.addEventListener("DOMContentLoaded", function () {
    initializeCode();
  });
}

function initializeCode() {}
setTimeout(async function getUsers(event) {
  const l = ["tervuren", "lhasa", "doberman", "malinois", "pug"];
  var d = document.createElement("div");
  d.className = "container";
  for (var i = 0; i < 5; i++) {
    const as = "https://dog.ceo/api/breed/" + l[i] + "/images/random";
    const wiki = "https://en.wikipedia.org/api/rest_v1/page/summary/" + l[i];
    const url = as;
    const usersPromise = await fetch(url);
    const user = await usersPromise.json();
    const usersPromise1 = await fetch(wiki);
    const user1 = await usersPromise1.json();

    var d1 = document.createElement("div");
    d1.className = "wiki-item";
    const name = l[i];
    var h1 = document.createElement("h1");
    h1.className = "wiki-header";
    h1.innerHTML = name;
    var d2 = document.createElement("div");
    d2.className = "wiki-content";
    var p = document.createElement("p");
    p.className = "wiki-text";
    p.innerHTML = user1.extract;
    var d3 = document.createElement("div");
    d3.className = "img-container";
    let t = document.createElement("img");
    t.className = "wiki-img";
    t.src = user.message;
    d1.appendChild(h1);
    d2.appendChild(p);
    d3.appendChild(t);
    d2.appendChild(d3);
    d1.appendChild(d2);
    d.appendChild(d1);
  }
  document.body.appendChild(d);

  return false;
});
