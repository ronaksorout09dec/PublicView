document.addEventListener('DOMContentLoaded', () => {
    // Sample course data
    const courses = [
        {
            id: 1,
            title: "Web Development Masterclass",
            description: "Learn HTML, CSS, and JavaScript from scratch",
            image: "https://www.cdmi.in/courses@2x/web-developments.webp",
            syllabus: [
                "Introduction to HTML",
                "CSS Fundamentals",
                "JavaScript Basics",
                "DOM Manipulation",
                "Project Building"
            ]
        },
        {
            id: 2,
            title: "Python for Beginners",
            description: "Master Python programming fundamentals",
            image: "https://miro.medium.com/v2/resize:fit:840/1*RJMxLdTHqVBSijKmOO5MAg.jpeg",
            syllabus: [
                "Python Syntax",
                "Data Structures",
                "Functions",
                "OOP Concepts",
                "Final Project"
            ]
        },
        {
            id: 3,
            title: "Java With DSA",
            description: "Master java programming fundamentals",
            image: "https://t3.ftcdn.net/jpg/04/51/12/88/360_F_451128839_vmKOyil368UoXcac46W7aaqelTtLuNFk.jpg",
            syllabus: [
                "JAVA Syntax",
                "Data Structures",
                "Functions",
                "OOP's Concepts",
                "Final Project"
            ]
        }
    ];

    // DOM Elements
    const courseContainer = document.getElementById('courseContainer');
    const courseTitle = document.getElementById('courseTitle');
    const syllabusContent = document.getElementById('syllabusContent');
    const enrollBtn = document.getElementById('enrollBtn');

    // Function to create course cards
    function createCourseCards() {
        courses.forEach(course => {
            const card = document.createElement('div');
            card.classList.add('course-card');
            card.innerHTML = `
                <img src="${course.image}" alt="${course.title}">
                <h3>${course.title}</h3>
                <p>${course.description}</p>
            `;
            
            card.addEventListener('click', () => showCourseDetails(course));
            courseContainer.appendChild(card);
        });
    }

    // Function to show course details
    function showCourseDetails(course) {
        courseTitle.textContent = course.title;
        syllabusContent.innerHTML = `
            <h4>Syllabus:</h4>
            <ul>
                ${course.syllabus.map(item => `<li>${item}</li>`).join('')}
            </ul>
        `;
        
        enrollBtn.onclick = () => {
            alert(`Enrolled in ${course.title}!`);
        };
    }

    // Initialize the page
    createCourseCards();
});