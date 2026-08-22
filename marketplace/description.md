# Tổng quan

EzyVector là plugin cơ sở dữ liệu vector tích hợp sẵn cho EzyPlatform. Bạn có thể lưu trữ và tìm kiếm vector (embedding) ngay trên chính website, không cần cài đặt thêm Qdrant, Pinecone hay Weaviate riêng. Phù hợp để xây dựng tìm kiếm ngữ nghĩa, gợi ý nội dung, hoặc làm nền tảng RAG cho trợ lý AI.

# Các tính năng chính

**Bộ sưu tập vector (collection)**
Tạo nhiều bộ sưu tập, mỗi bộ sưu tập có kích thước vector và độ đo khoảng cách riêng. Thêm/cập nhật từng điểm dữ liệu (point) kèm dữ liệu bổ sung (payload) qua API upsert.

**Chỉ mục HNSW cho tìm kiếm nhanh**
Tự động xây dựng chỉ mục HNSW (Hierarchical Navigable Small World) ở nền để tìm kiếm gần đúng với độ trễ thấp trên tập dữ liệu lớn. Trong lúc chỉ mục đang được xây dựng, hệ thống vẫn trả kết quả chính xác bằng tìm kiếm tuần tự nên tính năng tìm kiếm không bị gián đoạn.

**Tự backfill, không cần thao tác thủ công**
Khi bật chỉ mục cho một bộ sưu tập đã có dữ liệu, EzyVector tự động backfill toàn bộ điểm cũ vào kho vector và chỉ mục ở nền, không chặn các thao tác ghi/đọc đang diễn ra.

**Quản lý theo phân đoạn (segment)**
Dữ liệu được tổ chức theo phân đoạn mutable/immutable với trạng thái rõ ràng (đang xây dựng, đang hoạt động, đang hợp nhất, lỗi thời, bị lỗi), giúp theo dõi vòng đời index minh bạch.

**Bảo mật API theo khóa và IP**
Truy cập API bộ sưu tập vector được bảo vệ bằng API key riêng và danh sách IP được phép truy cập, hạn chế truy cập trái phép từ bên ngoài.

**Quản trị trực quan**
Trang quản trị EzyPlatform hiển thị danh sách bộ sưu tập, điểm dữ liệu, phân đoạn, loại và phiên bản chỉ mục, số lượng điểm, ID điểm nhỏ nhất/lớn nhất... giúp vận hành và theo dõi ngay trong admin quen thuộc, không cần công cụ ngoài.

# Yêu cầu

Website EzyPlatform đã cấu hình MySQL để lưu metadata bộ sưu tập/điểm/phân đoạn. Dữ liệu vector và chỉ mục HNSW được lưu trên hệ thống file của server.
